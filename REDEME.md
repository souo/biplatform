biplatform
-----------

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/06eb6fd6cfa948b3b7a2480b154c5cf6)](https://www.codacy.com/app/souo/biplatform?utm_source=github.com&utm_medium=referral&utm_content=souo/biplatform&utm_campaign=badger)
[![Build Status](https://travis-ci.org/souo/biplatform.svg?branch=master)](https://travis-ci.org/souo/biplatform)

基于scala 、akka实现了一个简单的报表工具。该项目是个玩具项目，用于个人学习scala和akka。

核心功能
------
* 每个用户、每张报表都作为一个独立的actor
* 通过akka Persistence持久化各个节点的状态。
* 使用kryo序列化消息
* 通过akka cluster支持横向扩展

接口文档
----
## 1、用户登入

*url*: /api/users/login

*method*:  post

*请求参数*：

|参数|名称|是否必须|参数类型|数据类型及范围|
|:---:|:---:|:---:|:---:|:---:|:---:|
|login|登录用户名|是|	body|	string||


*返回结果*

|参数|名称|数据类型及范围|
|:---:|:---:|:---:|
isAdmin||boolean

## 2、用户登出

*url*: /api/users/logout

*method*:  get

## 3、 获取所有表
*url*: /api/admin/tables

*method*:  get

*返回结果*

|参数|名称|数据类型及范围|
|:---:|:---:|:---:|
|id|||
|name|
|comment|

## 4、获取表所有字段

*url*:  /api/admin/tables/{{tableId}}/fieldsInfo

*method* :  get

*返回结果*

|参数|名称|数据类型及范围|
|:---:|:---:|:---:|
|id|||
|name|
|comment|
|dataType|

## 5、创建cube
**url** :  /api/admin/cubes

**method** :  post

**请求参数**

|参数|名称|是否必须|参数类型|数据类型及范围|
|:---:|:---:|:---:|:---:|:---:|:---:|
|name|cube名|是|	body|	string||
|schema||是|body||

schema 包含字段

|参数|名称|是否必须|数据类型及范围|
|:---:|:---:|:---:|:---:|:---:|:---:|
|tableName|表名|是|String|
|dimensions|维度|是| Array
|measures|指标|是|Array

维度指标字段：

|参数|名称|是否必须|数据类型及范围|
|:---:|:---:|:---:|:---:|:---:|:---:|
|name|字段名|是|string
|caption|字幕|是|string
|description|描述|否|string
|dataType|数据类型|是|string

返回结果 

|参数|名称|是否必须|数据类型及范围|
|:---:|:---:|:---:|:---:|:---:|:---:|
|cubeId|
|cubeName|
|createBy|
|modifyBy|
|lastModifyTime|

请求示例

```json
{
  "name": "test",
  "schema": {
    "tableName": "table",
    "dimensions": [
      {
        "name": "f1",
        "caption": "字段1",
        "dataType": "string"
      }
    ],
    "measures": [
      {
        "name": "f2",
        "caption": "字段2",
        "dataType": "string"
      }
    ]
  }
}
```
返回示例

```json
{
  "status": 200,
  "message": "OK",
  "data": {
    "cubeId": "85d1ec5c-b387-4b80-9da7-4979ba96a15f",
    "cubeName": "test",
    "createBy": "admin",
    "modifyBy": null,
    "lastModifyTime": "2016-12-15T11:06:41.295+08:00"
  }
}
```
## 6、获取cube元数据


**url** :  /api/admin/cubes

**method** :  GET

返回结果示例

```json
{
  "status": 200,
  "message": "OK",
  "data": [
    {
      "cubeId": "85d1ec5c-b387-4b80-9da7-4979ba96a15f",
      "cubeName": "test",
      "createBy": "admin",
      "modifyBy": null,
      "lastModifyTime": "2016-12-15T11:06:41.295+08:00"
    },
    {
      "cubeId": "08db5025-fc87-4f6b-9b38-e95c5dddf908",
      "cubeName": "test",
      "createBy": "admin",
      "modifyBy": null,
      "lastModifyTime": "2016-12-15T11:13:22.543+08:00"
    }
  ]
}
```

## 7、修改cube

**url** :  /api/admin/cubes/{{cubeId}}

**method** :  put

**请求参数**

|参数|名称|是否必须|参数类型|数据类型及范围|
|:---:|:---:|:---:|:---:|:---:|:---:|
|name|cube名|是|	body|	string||
|schema||是|body||

schema 包含字段

|参数|名称|是否必须|数据类型及范围|
|:---:|:---:|:---:|:---:|:---:|:---:|
|tableName|表名|是|String|
|dimensions|维度|是| Array
|measures|指标|是|Array

维度指标字段：

|参数|名称|是否必须|数据类型及范围|
|:---:|:---:|:---:|:---:|:---:|:---:|
|name|字段名|是|string
|caption|字幕|是|string
|description|描述|否|string
|dataType|数据类型|是|string

返回结果 

|参数|名称|是否必须|数据类型及范围|
|:---:|:---:|:---:|:---:|:---:|:---:|
|cubeId|
|cubeName|
|createBy|
|modifyBy|
|lastModifyTime|

## 8、删除cube

**url** :  /api/admin/cubes/{{cubeId}}

**method** :  delete


## 9、获取cube Schema

**url** :  /api/admin/cubes/{{cubeId}}/schema

**method** :  GET

请求示例

```
/api/admin/cubes/85d1ec5c-b387-4b80-9da7-4979ba96a15f/schema
```

返回示例

```json
{
  "status": 200,
  "message": "OK",
  "data": {
    "tableName": "table",
    "dimensions": [
      {
        "name": "f1",
        "caption": "字段1",
        "description": null,
        "dataType": "string"
      }
    ],
    "measures": [
      {
        "dimension": {
          "name": "f2",
          "caption": "字段2",
          "description": null,
          "dataType": "string"
        },
        "aggregator": "SUM"
      }
    ]
  }
}

```

## 10、创建报表


**url** :  /api/designer/reports

**method** :  POST

**请求参数：**

|参数|名称|是否必须|参数类型|数据类型及范围|
|:---:|:---:|:---:|:---:|:---:|:---:|
|name|报表名|是|	body|	string||

**返回结果示例**

```json
{
  "status": 200,
  "message": "OK",
  "data": [
    {
      "id": "4a66efa7-7b26-43fd-899a-294c2105955c",
      "name": "a",
      "createBy": "test",
      "createTime": "2016-12-27 13:03:49",
      "modifyBy": null,
      "latModifyTime": null
    }
  ]
}
```

## 11、获取报表
**url** :  /api/designer/reports

**method** :  GET

**返回结果示例**

```json
{
  "status": 200,
  "message": "OK",
  "data": [
    {
      "id": "4a66efa7-7b26-43fd-899a-294c2105955c",
      "name": "a",
      "createBy": "test",
      "createTime": "2016-12-27 13:03:49",
      "modifyBy": null,
      "latModifyTime": null
    }
  ]
}
```

## 12、更新报表

**url** :  /api/designer/reports

**method** :  PUT

**请求参数：**

|参数|名称|是否必须|参数类型|数据类型及范围|
|:---:|:---:|:---:|:---:|:---:|:---:|
|id|报表id|是|	body|	string||
|name|报表名|是|	body|	string||

**请求示例：**

```json
{
	"id": "4a66efa7-7b26-43fd-899a-294c2105955c",
	"name":"b"
}

```

**返回结果示例**

```json
{
  "status": 200,
  "message": "OK",
  "data": [
    {
      "id": "4a66efa7-7b26-43fd-899a-294c2105955c",
      "name": "b",
      "createBy": "test",
      "createTime": "2016-12-27 13:03:49",
      "modifyBy": "test",
      "latModifyTime": "2016-12-27 13:16:42"
    }
  ]
}
```

## 13、删除报表
**url** :  /api/designer/reports

**method** :  DELETE

**请求参数：**

|参数|名称|是否必须|参数类型|数据类型及范围|
|:---:|:---:|:---:|:---:|:---:|:---:|
|reportId|报表id|是|	path|	string||

**请求示例：**

```
 /api/designer/reports?reportId=7f1e1459-9094-43e4-b15f-f2010c252786

```

**返回结果示例**

```json
{
  "status": 200,
  "message": "OK",
  "data": []
}
```

## 14、 保存
**url** :  /api/designer/reports/{{reportId}}

**method** :  post

**请求参数：**

|参数|名称|是否必须|参数类型|数据类型及范围|
|:---:|:---:|:---:|:---:|:---:|:---:|
|queryModel||是|	body|	QueryModel||
|cubeId||是||String|
|properties|

**请求示例：**

```json
{
  "queryModel" : {
    "tableName" : "app_visit_week",
    "dimensions" : [
      {
        "name" : "visit_type",
        "caption" : "",
        "description" : null,
        "dataType" : "string"
      },
      {
        "name" : "source",
        "caption" : "",
        "description" : null,
        "dataType" : "string"
      }
    ],
    "measures" : [
      {
        "dimension" : {
          "name" : "uv",
          "caption" : "",
          "description" : null,
          "dataType" : "string"
        },
        "aggregator" : "SUM"  //"SUM"  "COUNT"  "AVG"  "DISTINCT_COUNT"
      },
      {
        "dimension" : {
          "name" : "pv",
          "caption" : "",
          "description" : null,
          "dataType" : "string"
        },
        "aggregator" : "COUNT"
      }
    ],
    "filters" : [
      {
        "lt" : {    //"gt" "gte" "lt"  "lte"  "eql"
          "dimension" : {
            "name" : "visit_typ",
            "caption" : "",
            "description" : null,
            "dataType" : "string"
          },
          "value" : "***"   //value type String
        }
      },
      {
        "in" : {  //"in"  "range"
          "dimension" : {
            "name" : "source",
            "caption" : "",
            "description" : null,
            "dataType" : "string"
          },
          "value" : [ //value type  Array[String]
            "a",
            "b"
          ]
        }
      }
    ]
  },
  "cubeId" : "d40fdcde-ba0b-4932-9ade-f840dce1be0a",
  "properties" : {
    "ui" : "TABLE"
  }
}
```

## 15、 获取

**url** :  /api/designer/reports/{{reportId}}

**method** :  GET

## 16、 清除

**url** :  /api/designer/reports/{{reportId}}

**method** :  DELETE

## 17、 执行

**url** :  /api/designer/reports/{{reportId}}/execute

**method** :  POST

|参数|名称|是否必须|参数类型|数据类型及范围|
|:---:|:---:|:---:|:---:|:---:|:---:|
|||是|	body|	QueryModel||

请求示例

```json
{
  "tableName" : "app_visit_week",
  "dimensions" : [
    {
      "name" : "visit_type",
      "caption" : "",
      "description" : null,
      "dataType" : "string"
    },
    {
      "name" : "source",
      "caption" : "",
      "description" : null,
      "dataType" : "string"
    }
  ],
  "measures" : [
    {
      "dimension" : {
        "name" : "uv",
        "caption" : "",
        "description" : null,
        "dataType" : "string"
      },
      "aggregator" : "SUM"
    },
    {
      "dimension" : {
        "name" : "pv",
        "caption" : "",
        "description" : null,
        "dataType" : "string"
      },
      "aggregator" : "COUNT"
    }
  ],
  "filters" : [
    {
      "lt" : {
        "dimension" : {
          "name" : "visit_typ",
          "caption" : "",
          "description" : null,
          "dataType" : "string"
        },
        "value" : "***"
      }
    },
    {
      "in" : {
        "dimension" : {
          "name" : "source",
          "caption" : "",
          "description" : null,
          "dataType" : "string"
        },
        "value" : [
          "a",
          "b"
        ]
      }
    }
  ]
}
```

返回结果

```json
{
  "rows" : [
    {
      "cells" : [
        {
          "value" : "visit_type",
          "type" : "ROW_HEADER_HEADER",
          "properties" : {
            "caption" : ""
          }
        },
        {
          "value" : "source",
          "type" : "ROW_HEADER_HEADER",
          "properties" : {
            "caption" : ""
          }
        },
        {
          "value" : "uv",
          "type" : "COLUMN_HEADER",
          "properties" : {
            "caption" : "uv求和"
          }
        },
        {
          "value" : "pv",
          "type" : "COLUMN_HEADER",
          "properties" : {
            "caption" : "pv计数"
          }
        }
      ]
    },
    {
      "cells" : [
        {
          "value" : "APP-Android",
          "type" : "ROW_HEADER",
          "properties" : {
            "dimension" : "visit_type"
          }
        },
        {
          "value" : "APPSTORE",
          "type" : "ROW_HEADER",
          "properties" : {
            "dimension" : "source"
          }
        },
        {
          "value" : "1",
          "type" : "DATA_CELL",
          "properties" : {
            "position" : "0:1"
          }
        },
        {
          "value" : "1",
          "type" : "DATA_CELL",
          "properties" : {
            "position" : "0:2"
          }
        }
      ]
    },
    {
      "cells" : [
        {
          "value" : "APP-IOS",
          "type" : "ROW_HEADER",
          "properties" : {
            "dimension" : "visit_type"
          }
        },
        {
          "value" : "APPSTORE",
          "type" : "ROW_HEADER",
          "properties" : {
            "dimension" : "source"
          }
        },
        {
          "value" : "80",
          "type" : "DATA_CELL",
          "properties" : {
            "position" : "1:1"
          }
        },
        {
          "value" : "2",
          "type" : "DATA_CELL",
          "properties" : {
            "position" : "1:2"
          }
        }
      ]
    }
  ]
}


```

## 18、 下载

**url** :  /api/designer/reports/download/{{reportId}}

**method** :  POST

|参数|名称|是否必须|参数类型|数据类型及范围|
|:---:|:---:|:---:|:---:|:---:|:---:|
|||是|	body|	QueryModel||






