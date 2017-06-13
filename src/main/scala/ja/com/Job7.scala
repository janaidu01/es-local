package ja.com

import java.io.File

import ja.com.Common.{cdxItem, _}
import ja.conf.JobSparkConf

object Job7 {

  def main(args: Array[String]): Unit = {
      // working copy for arc file
    val sourcePath = "C:\\Users\\Ja\\Downloads\\Archive\\EA-TNA-0709-biglotteryfund.org.uk-p-20090831083143-00000.arc.gz"
    val file = new File(sourcePath)

    val arcreader = org.archive.io.arc.ARCReaderFactory.get(file)
    val arcrecord = arcreader.get(215743)
    println(arcrecord.getHeader.getUrl)

    // working copy for cdx file
    val cdxpath = "C:\\Users\\Ja\\Downloads\\EA-TNA-0709-biglotteryfund.org.uk-p-20090831083143-00000.cdx"
    val cdxfile = new File(cdxpath)
    println(cdxfile)

    val txtRDD = JobSparkConf.sc.textFile(cdxpath, 1)
    val rddLines = txtRDD.mapPartitionsWithIndex { (idx, iter) => if (idx == 0) iter.drop(1) else iter }



    import JobSparkConf.sqlContext.implicits._
    val cdxItems2 = rddLines.map(x => cdxItem(x.split(" ")(0)
      ,x.split(" ")(1),x.split(" ")(2), x.split(" ")(3),
      x.split(" ")(4),x.split(" ")(5), x.split(" ")(6),
      x.split(" ")(7),x.split(" ")(8),x.split(" ")(9)
    )
    ).toDF()
    val cdxItems = cdxItems2.sort(cdxItems2("V_compressed_arc_file_offset"))
    val cdxItems_offsets = cdxItems.select($"V_compressed_arc_file_offset", $"massaged_url").persist()

    println("cdxItems_offsets count :" + cdxItems_offsets.count)

    val filteredCDXitems = cdxItems
      .filter(!(cdxItems("s_response_code").between("400", "511")))
      .withColumn("New_URL", toGetURLnew(cdxItems("a_origina_url")))

    // println(filteredCDXitems); filteredCDXitems.show(10,truncate = false); filteredCDXitems.printSchema()

     filteredCDXitems.registerTempTable("mainT")



  }
}
