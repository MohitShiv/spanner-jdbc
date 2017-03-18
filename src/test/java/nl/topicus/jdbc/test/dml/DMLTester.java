package nl.topicus.jdbc.test.dml;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;

import nl.topicus.jdbc.test.TestUtil;

/**
 * Class for testing INSERT, UPDATE, DELETE statements
 * 
 * @author loite
 *
 */
public class DMLTester
{
	private Connection connection;

	public DMLTester(Connection connection)
	{
		this.connection = connection;
	}

	public void runInsertAndUpdateTests() throws IOException, URISyntaxException, SQLException
	{
		runInsertTests();
		runUpdateTests();
	}

	@SuppressWarnings("deprecation")
	private void runInsertTests() throws IOException, URISyntaxException, SQLException
	{
		executeStatements("InsertIntoTest.sql");
		verifyTableContents("SELECT DESCRIPTION FROM TEST WHERE ID=1", "Description 1");
		verifyTableContents("SELECT CREATED_DATE FROM TEST WHERE ID=1", new Date(2017 - 1900, 2, 18));
		verifyTableContents("SELECT LAST_UPDATED FROM TEST WHERE ID=1", new Timestamp(2017 - 1900, 2, 18, 7, 0, 0, 0));
		verifyTableContents("SELECT ACTIVE FROM TEST WHERE ID=1", Boolean.TRUE);

		verifyTableContents("SELECT DESCRIPTION FROM TEST WHERE ID=2", "Description 2");
		verifyTableContents("SELECT AMOUNT FROM TEST WHERE ID=2", Float.valueOf(-29.95f));

		executeStatements("InsertIntoTestChild.sql");
		verifyTableContents("SELECT DESCRIPTION FROM TESTCHILD WHERE ID=1 AND CHILDID=1", "Child description 1.1");
	}

	@SuppressWarnings("deprecation")
	private void runUpdateTests() throws IOException, URISyntaxException, SQLException
	{
		executeStatements("UpdateTest.sql");
		verifyTableContents("SELECT UUID FROM TEST WHERE ID=1", new byte[] { (byte) 170, (byte) 187, (byte) 204 });
		verifyTableContents("SELECT ACTIVE FROM TEST WHERE ID=1", Boolean.FALSE);
		verifyTableContents("SELECT AMOUNT FROM TEST WHERE ID=1", Float.valueOf(129.95f));

		verifyTableContents("SELECT AMOUNT FROM TEST WHERE ID=2", Float.valueOf(-129.95f));
		verifyTableContents("SELECT CREATED_DATE FROM TEST WHERE ID=2", new Date(2017 - 1900, 2, 17));
		verifyTableContents("SELECT LAST_UPDATED FROM TEST WHERE ID=2", new Timestamp(2017 - 1900, 2, 17, 8, 0, 0, 0));

		executeStatements("UpdateTestChild.sql");
	}

	public void runDeleteTests()
	{
	}

	private void verifyTableContents(String sql, Object expectedValue) throws SQLException
	{
		ResultSet rs = connection.createStatement().executeQuery(sql);
		if (rs.next())
		{
			Object value = rs.getObject(1);
			if (!Objects.equals(value, expectedValue))
			{
				throw new SQLException("Expected value: " + String.valueOf(expectedValue) + ", found value: "
						+ String.valueOf(value));
			}
		}
		else
		{
			throw new SQLException("No records found");
		}
		rs.close();
	}

	private void executeStatements(String file) throws IOException, URISyntaxException, SQLException
	{
		String[] statements = TestUtil.getMultipleStatements(getClass(), file);
		for (String sql : statements)
		{
			connection.createStatement().executeUpdate(sql);
		}
	}

}
