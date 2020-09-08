package edu.northeastern.khoury;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

import org.sqlite.SQLiteConfig;
import org.sqlite.util.OSInfo;

import com.opencsv.CSVReader;

/**
 * Performs a SQLite diff on an included database
 * between a provided solution and a query via
 * supplied SQL
 * 
 * @author derbinsky
 */
public class SQLiteDiff {
	
	/**
	 * Path to reference SQLite database
	 */
	private static String DB_PATH = SQLiteDiff.class.getResource("Chinook_Sqlite_AutoIncrementPKs.sqlite").toString();
	
	/**
	 * Compares number, name, and order of
	 * columns against a supplied reference list
	 * 
	 * @param rs query result set
	 * @param expected expected column names, in order
	 * @return error, or null if identical
	 */
	private static String checkColumns(ResultSet rs, String[] expected) {
		int colCount;
		try {
			colCount = rs.getMetaData().getColumnCount();
		
			if (colCount != expected.length) {
				return String.format("Column count incorrect: expected=%d, actual=%d%n", expected.length, colCount);
			}
			
			for (int i=1; i<=colCount; i++) {
				if (!expected[i-1].equals(rs.getMetaData().getColumnLabel(i))) {
					return String.format("Column label #%d incorrect: expected=\"%s\", actual=\"%s\"%n", i, expected[i-1], rs.getMetaData().getColumnLabel(i));
				}
			}
		} catch (SQLException e) {
			return e.toString();
		} 
		
		return null;
	}
	
	/**
	 * Compares row values against a reference list
	 * 
	 * @param rs query result set
	 * @param expected expected row values, in order
	 * @return error, or null if identical
	 */
	private static String checkRow(ResultSet rs, String[] expected) {
		try {
			if (!rs.next()) {
				return "No row available";
			}
			
			for (int i=1; i<=expected.length; i++) {
				final String val = rs.getObject(i).toString();
				if (!val.equals(expected[i-1])) {
					return String.format("Value for <%s> incorrect: expected=\"%s\", actual=\"%s\"%n", rs.getMetaData().getColumnLabel(i), expected[i-1], val);
				}
			}
			
		} catch (SQLException e) {
			return e.toString();
		}
		
		return null;
	}
	
	/**
	 * Run the diff
	 * 
	 * @param args should be answer file path, then query file path, optionally followed with a single value for debug
	 * @throws ClassNotFoundException error finding JDBC driver
	 */
	public static void main(String[] args) throws ClassNotFoundException {
		
		if (args.length!=2 && args.length!=3) {
			System.out.println("Usage: java " + SQLiteDiff.class.getName() + " <answer file> <query file> [anything for debug info]");
			return;
		}
		
		final String answerFile = args[0];
		final String queryFile = args[1];
		final boolean debugInfo = (args.length == 3);
		
		System.out.printf("Trying to produce <%s> with <%s>%n%n", answerFile, queryFile);		
		System.out.printf("Using database... %n%s%n%n", DB_PATH);
		
		// Parse the query file
		String sql = null;
		try (final Scanner qIn = new Scanner(new InputStreamReader(new FileInputStream(queryFile), "UTF-8"))) {
			final StringBuilder sb = new StringBuilder();
			
			while (qIn.hasNextLine()) {
				final String line = qIn.nextLine().trim();
				if (!line.isEmpty()) {
					if (sb.length()!=0) {
						sb.append(" ");
					}
					sb.append(line);
				}
			}
			
			sql = sb.toString();
		} catch (Exception e) {
			System.out.printf("Query file exception: %s%n", e.toString());
			return;
		}
		System.out.printf("Using query...%n%s%n%n", sql);
		
		// Parse the answer CSV
		String[] cols = null;
		ArrayList<String[]> rows = new ArrayList<>();
		try (final CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(answerFile), "UTF-8"))) {
			String[] line;
			while ((line = reader.readNext()) != null) {
				if (cols == null) {
					cols = line;
				} else {
					rows.add(line);
				}
			}
		} catch (Exception e) {
			System.out.printf("Answer file exception: %s%n", e.toString());
			return;
		}
		
		// Perform diff
		Class.forName("org.sqlite.JDBC");
		Connection connection = null;
		try {
			
			// create a database connection
			final SQLiteConfig config = new SQLiteConfig();
			config.setReadOnly(true);
			connection = DriverManager.getConnection(String.format("jdbc:sqlite::resource:%s",DB_PATH), config.toProperties());
			
			if (debugInfo) {
				final DatabaseMetaData md = connection.getMetaData();
				
				System.out.printf("== Debug Info ==%n");
				System.out.printf("JDBC: v%d.%d%n", md.getJDBCMajorVersion(), md.getJDBCMinorVersion());
				System.out.printf("Driver: %s (%s) v%d.%d%n", md.getDriverName(), md.getDriverVersion(), md.getDriverMajorVersion(), md.getDriverMinorVersion());
				System.out.printf("Product: %s v%s%n", md.getDatabaseProductName(), md.getDatabaseProductVersion());
				System.out.printf("Database: v%d.%d%n", md.getDatabaseMajorVersion(), md.getDatabaseMinorVersion());
				System.out.printf("%s (%s)%n", OSInfo.getOSName(), OSInfo.getArchName());
				
				System.out.printf("%n");
			}
			
			//
			
			System.out.printf("== Result ==%n");
			
			final PreparedStatement stmt = connection.prepareStatement(sql);
			final ResultSet res = stmt.executeQuery();
			
			System.out.print("Checking columns... ");
			{
				final String output = checkColumns(res, cols);
				if (output == null) {
					System.out.println("same");
				} else {
					System.out.print(output);
					return;
				}
			}
			
			for (int i=0; i<rows.size(); i++) {
				final String[] row = rows.get(i);
				
				System.out.printf("Checking row #%d... ", i+1);
				final String output = checkRow(res, row);
				if (output == null) {
					System.out.println("same");
				} else {
					System.out.print(output);
					return;
				}
			}
			
			System.out.printf("%nQuery correctly reproduces answer file.%n");
			
		} catch (SQLException e) {
			System.err.println(e);
		} finally {
			try {
				if (connection != null) {
					connection.close();
				}
			} catch (SQLException e) {
				System.err.println( e );
			}
		}
	}
}
