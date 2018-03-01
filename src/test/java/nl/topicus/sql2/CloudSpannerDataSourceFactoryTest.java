package nl.topicus.sql2;

import static nl.topicus.sql2.connectionproperty.CloudSpannerCredentialsPathConnectionProperty.PVTKEYPATH;
import static nl.topicus.sql2.connectionproperty.CloudSpannerDatabaseConnectionProperty.DATABASE;
import static nl.topicus.sql2.connectionproperty.CloudSpannerInstanceConnectionProperty.INSTANCE;
import static nl.topicus.sql2.connectionproperty.CloudSpannerProjectConnectionProperty.PROJECT;
import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.Instance;
import com.google.cloud.spanner.InstanceAdminClient;
import com.google.cloud.spanner.InstanceConfig;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.Operation;
import com.google.cloud.spanner.Spanner;
import com.google.spanner.admin.database.v1.CreateDatabaseMetadata;
import com.google.spanner.admin.instance.v1.CreateInstanceMetadata;

import nl.topicus.java.sql2.Submission;
import nl.topicus.jdbc.test.category.UnitTest;

@Category(UnitTest.class)
public class CloudSpannerDataSourceFactoryTest
{
	private static final class PrintErrorMessageHandler implements Function<Throwable, Void>
	{
		public static final PrintErrorMessageHandler INSTANCE = new PrintErrorMessageHandler();

		private PrintErrorMessageHandler()
		{
		}

		@Override
		public Void apply(Throwable t)
		{
			t.printStackTrace(System.err);
			return null;
		}

	}

	private static final String PROJECT_ID = "gothic-calling-193809";
	private static final String INSTANCE_ID = "test-instance";
	private static final String DATABASE_ID = "test";

	@Test
	public void testFactory() throws InterruptedException, ExecutionException, SQLException
	{
		CloudSpannerDataSourceFactory factory = CloudSpannerDataSourceFactory.INSTANCE;
		CloudSpannerDataSource ds = factory.builder().connectionProperty(PROJECT, PROJECT_ID)
				.connectionProperty(INSTANCE, INSTANCE_ID).connectionProperty(DATABASE, DATABASE_ID)
				.connectionProperty(PVTKEYPATH, "cloudspanner-key.json").build();
		Spanner spanner = null;
		try (CloudSpannerConnection connection = ds.getConnection(PrintErrorMessageHandler.INSTANCE))
		{
			connection.waitForConnect(10000L, TimeUnit.MILLISECONDS);
			spanner = connection.getSpanner();
			createInstance(spanner, PROJECT_ID, INSTANCE_ID);
			createDatabase(spanner, INSTANCE_ID, DATABASE_ID);

			connection.<Long> countOperation(
					"create table wordcount (word string(100) not null, count int64 not null) primary key (word)")
					.submit();
			Submission<Long> res = connection.<Long> countOperation("insert into wordcount (word, count) values (?, ?)")
					.set("1", "test").set("2", 100).submit();
			long count = res.toCompletableFuture().get();
			assertEquals(1l, count);
		}
		finally
		{
			if (spanner != null)
			{
				cleanUpDatabase(spanner, INSTANCE_ID, DATABASE_ID);
				cleanUpInstance(spanner, INSTANCE_ID);
			}
		}
	}

	private void createInstance(Spanner spanner, String projectId, String instanceId)
	{
		InstanceAdminClient instanceAdminClient = spanner.getInstanceAdminClient();
		InstanceConfig config = instanceAdminClient.getInstanceConfig("regional-europe-west1");
		Instance instance = instanceAdminClient.newInstanceBuilder(InstanceId.of(projectId, instanceId))
				.setDisplayName("Test Instance").setInstanceConfigId(config.getId()).setNodeCount(1).build();
		Operation<Instance, CreateInstanceMetadata> createInstance = instanceAdminClient.createInstance(instance);
		createInstance = createInstance.waitFor();
	}

	private void createDatabase(Spanner spanner, String instanceId, String databaseId)
	{
		Operation<Database, CreateDatabaseMetadata> createDatabase = spanner.getDatabaseAdminClient()
				.createDatabase(instanceId, databaseId, Arrays.asList());
		createDatabase = createDatabase.waitFor();
	}

	private void cleanUpInstance(Spanner spanner, String instanceId)
	{
		spanner.getInstanceAdminClient().deleteInstance(instanceId);
	}

	private void cleanUpDatabase(Spanner spanner, String instanceId, String databaseId)
	{
		spanner.getDatabaseAdminClient().dropDatabase(instanceId, databaseId);
	}

}