package com.example.documentqa;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Connection details for SAP HANA Cloud, read from environment variables instead of
// application.properties, because that file is on GitHub and the password must never be.
//   HANA_ENDPOINT  the instance's SQL endpoint, host:port (from HANA Cloud Central)
//   HANA_USER      the app's own database user (DOCUMENTQA, not DBADMIN: least privilege)
//   HANA_PASSWORD  that user's password
public record HanaSettings(String endpoint, String user, String password) {

	private static final String[] VARIABLES = { "HANA_ENDPOINT", "HANA_USER", "HANA_PASSWORD" };

	// True when all three variables are set. Integration tests use this to skip
	// (not fail) on machines without HANA access, such as GitHub Actions
	public static boolean isConfigured() {
		for (String name : VARIABLES) {
			String value = System.getenv(name);
			if (value == null || value.isBlank()) {
				return false;
			}
		}
		return true;
	}

	// Fails with the name of the missing variable, instead of a confusing
	// connection error later
	public static HanaSettings fromEnvironment() {
		return new HanaSettings(require("HANA_ENDPOINT"), require("HANA_USER"), require("HANA_PASSWORD"));
	}

	private static String require(String name) {
		String value = System.getenv(name);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException("Environment variable " + name + " is not set");
		}
		return value.trim();
	}

	// HANA Cloud only accepts encrypted connections. validateCertificate=true checks
	// we are really talking to SAP's server, not something pretending to be it
	public String jdbcUrl() {
		return "jdbc:sap://" + endpoint + "/?encrypt=true&validateCertificate=true";
	}

	// The caller must close the connection (try-with-resources)
	public Connection connect() throws SQLException {
		return DriverManager.getConnection(jdbcUrl(), user, password);
	}

	// A record's default toString prints every field, which would put the password
	// into logs or error messages. This version hides it
	@Override
	public String toString() {
		return "HanaSettings[endpoint=" + endpoint + ", user=" + user + ", password=****]";
	}
}
