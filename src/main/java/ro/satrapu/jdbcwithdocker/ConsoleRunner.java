package ro.satrapu.jdbcwithdocker;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Connects to a RDBMS instance using JDBC and executes a query.
 *
 * @author satrapu
 */
public class ConsoleRunner {

	/**
	 * Represents the JDBC URL pointing to a RDBMS, e.g. MySQL. Example:
	 * "jdbc:mysql://address=(protocol=tcp)(host=localhost)(port=3306)/jdbc-with-docker?useSSL=false".
	 */
	private static final String ENV_VARS_JDBC_URL = "JDBC_URL";
	private static final String ENV_VARS_JDBC_USER = "JDBC_USER";
	private static final String ENV_VARS_JDBC_PASSWORD = "JDBC_PASSWORD";

	private static final String JDBC_PROPERTIES_USER = "user";
	private static final String JDBC_PROPERTIES_PASSWORD = "password";

	private static final String COLUMNS_TABLE_SCHEMA = "TABLE_SCHEMA";
	private static final String COLUMNS_TABLE_NAME = "TABLE_NAME";
	private static final String COLUMNS_TABLE_TYPE = "TABLE_TYPE";

	private static final String TABLE_SEPARATOR = "--------------------------------------------------------------------------------------------------------------";

	private static final String SELECT_TABLE_INFO = "SELECT T.TABLE_SCHEMA, T.TABLE_NAME, T.TABLE_TYPE FROM INFORMATION_SCHEMA.TABLES T ORDER BY 1 ASC, 2 ASC, 3 ASC";

	public static void main(String[] args) {
		String jdbcUrl = getEnvironmentVariable(ENV_VARS_JDBC_URL);
		String jdbcUsername = getEnvironmentVariable(ENV_VARS_JDBC_USER);
		String jdbcPassword = getEnvironmentVariable(ENV_VARS_JDBC_PASSWORD);

		Driver jdbcDriver = null;

		try {
			jdbcDriver = DriverManager.getDriver(jdbcUrl);
		} catch (SQLException e) {
			throw new RuntimeException(
					String.format("Could not find any suitable JDBC driver using URL: \"%s\".", jdbcUrl), e);
		}

		Properties jdbcInfo = new Properties();
		jdbcInfo.put(JDBC_PROPERTIES_USER, jdbcUsername);
		jdbcInfo.put(JDBC_PROPERTIES_PASSWORD, jdbcPassword);

		try (Connection jdbcConnection = jdbcDriver.connect(jdbcUrl, jdbcInfo)) {
			try (Statement jdbcStatement = jdbcConnection.createStatement()) {
				try (ResultSet jdbcResultSet = jdbcStatement.executeQuery(SELECT_TABLE_INFO)) {
					System.out.println(TABLE_SEPARATOR);
					System.out.printf("| %25s | %50s | %25s |\n", COLUMNS_TABLE_SCHEMA, COLUMNS_TABLE_NAME,
							COLUMNS_TABLE_TYPE);
					System.out.println(TABLE_SEPARATOR);

					while (jdbcResultSet.next()) {
						String tableSchema = jdbcResultSet.getObject(COLUMNS_TABLE_SCHEMA, String.class);
						String tableName = jdbcResultSet.getObject(COLUMNS_TABLE_NAME, String.class);
						String tableType = jdbcResultSet.getObject(COLUMNS_TABLE_TYPE, String.class);

						System.out.printf("| %25s | %50s | %25s |\n", tableSchema, tableName, tableType);
					}

					System.out.println(TABLE_SEPARATOR);
				} catch (SQLException e) {
					throw new RuntimeException(String.format("Failed to execute query: \"%s\".", SELECT_TABLE_INFO), e);
				}
			} catch (SQLException e) {
				throw new RuntimeException("Failed to create statement.", e);
			}
		} catch (SQLException e) {
			throw new RuntimeException(
					String.format("Could not connect to the underlying database using JDBC URL: \"%s\".", jdbcUrl), e);
		}
	}

	private static String getEnvironmentVariable(String name) {
		String value = System.getenv(name);
		System.out.printf("%s=\"%s\"\n\n", name, value);

		if (value == null || value.isEmpty()) {
			throw new RuntimeException(
					String.format("Could not find a non-empty environment variable named: \"%s\".", name));
		}

		return value;
	}
}
