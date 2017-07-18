# report api

## 保存

保存并验证用户对报表做的修改

```
POST  /api/designer/reports/{{reportId}}
```

Parameters

- reportId (required) 报表ID

Body

+ queryModel:
    - tableName:  String
    + dimensions: List[Dimension]
        - name:        String
        - caption:     String
        - description: Option[String]
        - dataType:    String
    + measures:   List[Measure]
        + dimension:
            - name:        String
            - caption:     String
            - description: Option[String]
            - dataType:    String
        + aggregator:   String  [SUM | COUNT | AVG | DISTINCT_COUNT]
    - filters:    Option[List[Filter]]
- cubeId: String
- properties:Map[String,String]


输入示例：

```
{
  "queryModel" : {
    "tableName" : "test",
    "dimensions": [
      {
        "name": "sex",
        "caption": "性别",
        "description": null,
        "dataType": "STRING"
      }
    ],
    "measures": [
      {
        "dimension": {
          "name": "degree",
          "caption": "得分",
          "description": null,
          "dataType": "NUMERIC"
        },
         "aggregator": "SUM"
        }
      ],
      "filters" : [
            {
              "lt" : {    //"gt" "gte" "lt"  "lte"  "eql"
                "dimension" : {
                  "name" : "visit_typ",
                  "caption" : "",
                  "description" : null,
                  "dataType" : "STRING"
                },
                "value" : "***"   //value type String
              }
            },
            {
              "in" : {  //"in"  
                "dimension" : {
                  "name" : "source",
                  "caption" : "",
                  "description" : null,
                  "dataType" : "STRING"
                },
                "value" : [ //value type  Array[String]
                  "a",
                  "b"
                ],
                "include": "Boolean"
              }
            },
             {  
             "range" : {
                 "dimension" : {
                    "name" : "source",
                     "caption" : "",
                     "description" : null,
                     "dataType" : "STRING"
                     },
                 "value" : [ //value type  Array[String]
                     a",
                     "b" 
                     ]
                 }
             }
          ]
    },
  "cubeId" : "1416ef1f-31b4-4aa8-adde-c8c9c9815e35",
  "properties" : {
    "ui" : "TABLE",
    "key1" : "value1",
    "key2" : "value2"
  }
}
```

输出：

```
{
  "status": 200,
  "message": "OK",
  "data": null
}
```

## 发布

```
GET  /api/designer/reports/publish/{{reportId}}
```

Parameters

- reportId (required) 报表ID

成功:

```
{
  "status": 200,
  "message": "OK",
  "data": null
}
```


失败:

```
{
  "status": 5000,
  "message": "错误",
  "moreInfo": "不能发布一张空的报表"
}
```

## 执行

```
GET  /api/designer/reports/execute/{{reportId}}
```

Parameters

- reportId (required) 报表ID

成功
```
{
  "status": 200,
  "message": "OK",
  "data": [
    {
      "cells": [
        {
          "value": "sex",
          "type": "ROW_HEADER_HEADER",
          "properties": {
            "caption": "性别"
          }
        },
        {
          "value": "degree",
          "type": "COLUMN_HEADER",
          "properties": {
            "caption": "得分求和"
          }
        }
      ]
    },
    {
      "cells": [
        {
          "value": "0",
          "type": "ROW_HEADER",
          "properties": {
            "dimension": "sex"
          }
        },
        {
          "value": "77.2",
          "type": "DATA_CELL",
          "properties": {
            "position": "0:1"
          }
        }
      ]
    },
    {
      "cells": [
        {
          "value": "1",
          "type": "ROW_HEADER",
          "properties": {
            "dimension": "sex"
          }
        },
        {
          "value": "58.2",
          "type": "DATA_CELL",
          "properties": {
            "position": "1:1"
          }
        }
      ]
    }
  ]
}
```

失败
```
{
  "status": 5000,
  "message": "错误",
  "moreInfo": "不能执行一张空的报表"
}
```

## 下载

```
GET  /api/designer/reports/download/{{reportId}}
```

Parameters

- reportId (required) 报表ID


## 获取最后一次保存的报表状态
```
GET  /api/designer/reports/{{reportId}}
```

```
{
  "queryModel" : {
    "tableName" : "test",
    "dimensions": [
      {
        "name": "sex",
        "caption": "性别",
        "description": null,
        "dataType": "STRING"
      }
    ],
    "measures": [
      {
        "dimension": {
          "name": "degree",
          "caption": "得分",
          "description": null,
          "dataType": "NUMERIC"
        },
         "aggregator": "SUM"
        }
      ],
      "filters" : [
            {
              "lt" : {    //"gt" "gte" "lt"  "lte"  "eql"
                "dimension" : {
                  "name" : "visit_typ",
                  "caption" : "",
                  "description" : null,
                  "dataType" : "STRING"
                },
                "value" : "***"   //value type String
              }
            },
            {
              "in" : {  //"in"  
                "dimension" : {
                  "name" : "source",
                  "caption" : "",
                  "description" : null,
                  "dataType" : "STRING"
                },
                "value" : [ //value type  Array[String]
                  "a",
                  "b"
                ],
                "include": "Boolean"
              }
            },
             {  
             "range" : {
                 "dimension" : {
                    "name" : "source",
                     "caption" : "",
                     "description" : null,
                     "dataType" : "STRING"
                     },
                 "value" : [ //value type  Array[String]
                     a",
                     "b" 
                     ]
                 }
             }
          ]
    },
  "cubeId" : "1416ef1f-31b4-4aa8-adde-c8c9c9815e35",
  "properties" : {
    "ui" : "TABLE",
    "key1" : "value1",
    "key2" : "value2"
  }
}
```

## 获取维度值

```
GET  /api/designer/reports/{{reportId}}/dim/{{dimame}}/values
```

Parameters

- reportId (required) 报表ID
- dimnameN (required) 维度名
