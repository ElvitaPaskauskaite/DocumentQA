package com.example.documentqa;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// A VectorStore backed by SAP HANA Cloud's vector engine. Vectors live in a REAL_VECTOR
// column and the database ranks them with COSINE_SIMILARITY, instead of the Java loop in
// InMemoryVectorStore. SemanticSearchService only sees the VectorStore interface, so
// nothing else changes when this store is used.
//
// Keeps one connection open, because each new connection means a new encrypted handshake
// with the server. Like the rest of the app, it is meant for one user at a time.
public class HanaVectorStore implements VectorStore, AutoCloseable {

	private final Connection connection;
	private final String table;
	private final int dimensions;

	// How a vector is passed into SQL. A bare TO_REAL_VECTOR(?) is read as the text
	// version ('[1,0]'), so the bytes arrived as an empty string. Casting the ? to
	// VARBINARY makes HANA use the binary version. VARBINARY allows up to 5000 bytes:
	// enough for 768 numbers (4 + 768 * 4 = 3076), not for much larger embeddings
	private static final String VECTOR_PARAMETER = "TO_REAL_VECTOR(CAST(? AS VARBINARY(5000)))";

	// Equal scores are returned in the order chunks were first added, like the LinkedHashMap
	// in InMemoryVectorStore, so both stores give identical results
	private long nextOrder;

	public HanaVectorStore(HanaSettings settings, String table, int dimensions) throws SQLException {
		// The table name is placed directly in SQL text (it can't be a ? parameter),
		// so only plain names are allowed, which rules out SQL injection through it
		if (!table.matches("[A-Z][A-Z0-9_]*")) {
			throw new IllegalArgumentException("Table name must be upper-case letters, digits or _: " + table);
		}
		this.table = table;
		this.dimensions = dimensions;
		this.connection = settings.connect();
		createTableIfMissing();
		nextOrder = highestOrder() + 1;
	}

	@Override
	public void add(TextChunk chunk, double[] vector) {
		checkDimensions(vector);
		try {
			// Same key again: replace the text and vector but keep its original order,
			// as LinkedHashMap.put does. Only a new key gets a new row
			String update = "UPDATE " + table + " SET SOURCE_NAME = ?, POSITION = ?, CONTENT = ?, "
					+ "EMBEDDING = " + VECTOR_PARAMETER + " WHERE CHUNK_KEY = ?";
			try (PreparedStatement statement = connection.prepareStatement(update)) {
				statement.setString(1, chunk.getSourceName());
				statement.setInt(2, chunk.getPosition());
				statement.setString(3, chunk.getContent());
				statement.setBytes(4, toBytes(vector));
				statement.setString(5, chunk.getKey());
				if (statement.executeUpdate() > 0) {
					return;
				}
			}

			String insert = "INSERT INTO " + table
					+ " (CHUNK_KEY, SOURCE_NAME, POSITION, CONTENT, EMBEDDING, INSERT_ORDER) "
					+ "VALUES (?, ?, ?, ?, " + VECTOR_PARAMETER + ", ?)";
			try (PreparedStatement statement = connection.prepareStatement(insert)) {
				statement.setString(1, chunk.getKey());
				statement.setString(2, chunk.getSourceName());
				statement.setInt(3, chunk.getPosition());
				statement.setString(4, chunk.getContent());
				statement.setBytes(5, toBytes(vector));
				statement.setLong(6, nextOrder++);
				statement.executeUpdate();
			}
		} catch (SQLException e) {
			throw failed("add " + chunk.getKey(), e);
		}
	}

	@Override
	public List<EmbeddingScore> search(double[] queryVector, int topK) {
		checkDimensions(queryVector);
		List<EmbeddingScore> results = new ArrayList<>();
		if (topK <= 0) {
			return results;
		}

		// HANA computes every similarity and returns only the best topK rows.
		// topK is an int, so putting it in the SQL text is safe
		String sql = "SELECT TOP " + topK + " SOURCE_NAME, POSITION, CONTENT, "
				+ "COSINE_SIMILARITY(EMBEDDING, " + VECTOR_PARAMETER + ") AS SCORE "
				+ "FROM " + table + " ORDER BY SCORE DESC, INSERT_ORDER ASC";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setBytes(1, toBytes(queryVector));
			try (ResultSet rows = statement.executeQuery()) {
				while (rows.next()) {
					TextChunk chunk = new TextChunk(rows.getString("SOURCE_NAME"),
							rows.getInt("POSITION"), rows.getString("CONTENT"));
					results.add(new EmbeddingScore(chunk, rows.getDouble("SCORE")));
				}
			}
		} catch (SQLException e) {
			throw failed("search", e);
		}
		return results;
	}

	@Override
	public int size() {
		try (Statement statement = connection.createStatement();
			 ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
			result.next();
			return result.getInt(1);
		} catch (SQLException e) {
			throw failed("count", e);
		}
	}

	@Override
	public void clear() {
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("DELETE FROM " + table);
		} catch (SQLException e) {
			throw failed("clear", e);
		}
	}

	@Override
	public void close() throws SQLException {
		connection.close();
	}

	// Created on first use, in the app user's own schema. A table that already exists
	// is kept, which is what makes the vectors survive a restart
	private void createTableIfMissing() throws SQLException {
		String check = "SELECT COUNT(*) FROM SYS.TABLES WHERE SCHEMA_NAME = CURRENT_SCHEMA AND TABLE_NAME = ?";
		try (PreparedStatement statement = connection.prepareStatement(check)) {
			statement.setString(1, table);
			try (ResultSet result = statement.executeQuery()) {
				result.next();
				if (result.getInt(1) > 0) {
					return;
				}
			}
		}
		try (Statement statement = connection.createStatement()) {
			statement.execute("CREATE COLUMN TABLE " + table + " ("
					+ "CHUNK_KEY NVARCHAR(500) PRIMARY KEY, "
					+ "SOURCE_NAME NVARCHAR(255) NOT NULL, "
					+ "POSITION INTEGER NOT NULL, "
					+ "CONTENT NCLOB NOT NULL, "
					+ "EMBEDDING REAL_VECTOR(" + dimensions + ") NOT NULL, "
					+ "INSERT_ORDER BIGINT NOT NULL)");
		}
	}

	private long highestOrder() throws SQLException {
		try (Statement statement = connection.createStatement();
			 ResultSet result = statement.executeQuery("SELECT COALESCE(MAX(INSERT_ORDER), 0) FROM " + table)) {
			result.next();
			return result.getLong(1);
		}
	}

	// A clear message here beats a database error about vector dimensions
	private void checkDimensions(double[] vector) {
		if (vector.length != dimensions) {
			throw new IllegalArgumentException("Expected " + dimensions + " numbers, got " + vector.length);
		}
	}

	// HANA's binary vector format: the number of values as a 4-byte int, then each value
	// as a 4-byte float, all little-endian. Bytes avoid turning 768 numbers into text
	// (long strings, rounding, and Java's "1.0E-5" notation)
	private static byte[] toBytes(double[] vector) {
		ByteBuffer buffer = ByteBuffer.allocate(4 + 4 * vector.length).order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(vector.length);
		for (double value : vector) {
			buffer.putFloat((float) value);
		}
		return buffer.array();
	}

	// VectorStore's methods don't declare SQLException, so it's wrapped in an unchecked
	// exception that says what was being done
	private static IllegalStateException failed(String action, SQLException e) {
		return new IllegalStateException("HANA " + action + " failed: " + e.getMessage(), e);
	}
}
