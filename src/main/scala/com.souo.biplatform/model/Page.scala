package com.souo.biplatform.model

/**
 * @author souo
 */
case class Page(pageNo: Int, pageSize: Int) {
  require(pageNo > 0, s"无效的页码:$pageNo")
  require(pageSize > 0, s"无效的分页大小:$pageSize")
}
