package sparkCassandra

import org.apache.spark.sql.functions._
import com.datastax.spark.connector._
import org.apache.spark.sql.cassandra._
import org.apache.spark.sql.SparkSession

object Leaves {

    def main(args: Array[String]){

        val username = ""
        val password = ""
        val masterURL = ""
        val dbName = ""
        val keyspace = ""

        val spark = SparkSession
            .builder()
            .appName("Leaves")
            .master(masterURL)
            .config("spark.cassandra.connection.config.cloud.path", s"secure-connect-$dbName.zip")
            .config("spark.cassandra.auth.username", username)
            .config("spark.cassandra.auth.password", password)
            .config("spark.sql.extensions", "com.datastax.spark.connector.CassandraSparkExtensions")
            .getOrCreate()

        spark.conf.set(s"spark.sql.catalog.$dbName", "com.datastax.spark.connector.datasource.CassandraCatalog")

        import spark.implicits._
        
        spark.sql(s"use $dbName.$keyspace")

        val leavesByTag = spark.sql("select tags as tag, title, url, tags from leaves").withColumn("tag", explode($"tag"))
        val tagsDF = spark.sql("select tags as tag from leaves").withColumn("tag", explode($"tag")).groupBy("tag").count()

        leavesByTag.createCassandraTable(keyspace, "leaves_by_tag", partitionKeyColumns = Some(Seq("tag")), clusteringKeyColumns = Some(Seq("title")))
        leavesByTag.write.cassandraFormat("leaves_by_tag", keyspace).mode("append").save()

        tagsDF.createCassandraTable(keyspace, "tags")
        tagsDF.write.cassandraFormat("tags", keyspace).mode("append").save()

        spark.stop()
    }
}
