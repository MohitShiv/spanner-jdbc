package nl.topicus.jdbc.statement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import nl.topicus.jdbc.CloudSpannerConnection;
import nl.topicus.jdbc.test.category.UnitTest;
import nl.topicus.jdbc.test.util.CloudSpannerTestObjects;

@Category(UnitTest.class)
public class CloudSpannerStatementTest
{
	private static final String INSERT_SQL = "INSERT INTO FOO (COL1, COL2, COL3) VALUES (1, 'two', 0xaa)";

	private static final String[] COLUMN_NAMES = new String[] { "COL1", "COL2", "COL3" };

	private static final int[] COLUMN_INDICES = new int[] { 1, 2, 3 };

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private CloudSpannerConnection createConnection() throws SQLException
	{
		CloudSpannerConnection connection = CloudSpannerTestObjects.createConnection();
		Mockito.when(connection.createStatement()).thenCallRealMethod();
		Mockito.when(connection.prepareStatement(Mockito.anyString())).thenCallRealMethod();

		return connection;
	}

	@Test
	public void testSelect() throws SQLException
	{
		String sql = "SELECT * FROM FOO";
		CloudSpannerConnection connection = createConnection();
		CloudSpannerStatement statement = connection.createStatement();
		boolean isResultSet = statement.execute(sql);
		Assert.assertTrue(isResultSet);
		ResultSet rs = statement.getResultSet();
		Assert.assertNotNull(rs);
		boolean moreResults = statement.getMoreResults();
		Assert.assertFalse(moreResults);
		Assert.assertTrue(rs.isClosed());
		Assert.assertEquals(-1, statement.getUpdateCount());

		ResultSet rs2 = statement.executeQuery("SELECT * FROM FOO");
		Assert.assertNotNull(rs2);
		Assert.assertFalse(statement.getMoreResults(Statement.KEEP_CURRENT_RESULT));
		Assert.assertFalse(rs2.isClosed());
	}

	@Test
	public void testInsert() throws SQLException
	{
		CloudSpannerConnection connection = createConnection();
		CloudSpannerStatement statement = connection.createStatement();
		boolean isResultSet = statement.execute(INSERT_SQL);
		Assert.assertFalse(isResultSet);
		int count = statement.getUpdateCount();
		Assert.assertEquals(1, count);
		boolean moreResults = statement.getMoreResults();
		Assert.assertFalse(moreResults);
		Assert.assertEquals(-1, statement.getUpdateCount());

		int count2 = statement.executeUpdate(INSERT_SQL);
		Assert.assertEquals(1, count2);
	}

	private CloudSpannerStatement prepareUnsupportedStatementTest() throws SQLException
	{
		thrown.expect(SQLFeatureNotSupportedException.class);
		CloudSpannerConnection connection = createConnection();
		CloudSpannerStatement statement = connection.createStatement();

		return statement;
	}

	@Test
	public void testExecuteAutoGeneratedKeys() throws SQLException
	{
		prepareUnsupportedStatementTest().execute(INSERT_SQL, 0);
	}

	@Test
	public void testExecuteColumnIndices() throws SQLException
	{
		prepareUnsupportedStatementTest().execute(INSERT_SQL, COLUMN_INDICES);
	}

	@Test
	public void testExecuteColumnNames() throws SQLException
	{
		prepareUnsupportedStatementTest().execute(INSERT_SQL, COLUMN_NAMES);
	}

	@Test
	public void testExecuteUpdateAutoGeneratedKeys() throws SQLException
	{
		prepareUnsupportedStatementTest().executeUpdate(INSERT_SQL, 0);
	}

	@Test
	public void testExecuteUpdateColumnIndices() throws SQLException
	{
		prepareUnsupportedStatementTest().executeUpdate(INSERT_SQL, COLUMN_INDICES);
	}

	@Test
	public void testExecuteUpdateColumnNames() throws SQLException
	{
		prepareUnsupportedStatementTest().executeUpdate(INSERT_SQL, COLUMN_NAMES);
	}

	@Test
	public void testIsDDLStatement() throws SQLException
	{
		CloudSpannerConnection connection = createConnection();
		CloudSpannerStatement statement = connection.createStatement();
		Assert.assertTrue(statement
				.isDDLStatement("CREATE TABLE FOO (ID INT64 NOT NULL, COL1 STRING(100) NOT NULL) PRIMARY KEY (ID)"));
		Assert.assertTrue(statement.isDDLStatement("ALTER TABLE FOO ADD COLUMN COL2 STRING(100) NOT NULL"));
		Assert.assertTrue(statement.isDDLStatement("DROP TABLE FOO"));
		Assert.assertTrue(statement.isDDLStatement("CREATE INDEX IDX_FOO ON FOO (COL1)"));
		Assert.assertTrue(statement.isDDLStatement("DROP INDEX IDX_FOO"));
	}

}