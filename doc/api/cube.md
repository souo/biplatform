# cube api

## 创建cube
```
POST /api/cubes
```

输入： application/json

输出： application/json


Body:

- cubeName: String （必须）  cube 名
- schema: Schema （必须） cube结构
  - tableName:      String  （必须）
  - dimensions:   List[Dimension] （必须）
      - name:        String （必须）
      - caption:     string  （必须)  中文名 
      - description: Option[String] 描述 
      - dataType:    String (必须）[STRING | NUMERIC | DATE]
  - measures:     List[Dimension] （必须）
      - name:        String （必须）
      - caption:     string  （必须)  中文名 
      - description: Option[String] 描述 
      - dataType:    String (必须）
  - dataSourceId: UUID （必须）


输入：

```
 {
  "cubeName": "test",
  "schema": {
    "tableName": "test",
    "dimensions": [
      {
        "name": "sex",
        "caption": "性别",
        "dataType": "STRING"
      }
    ],
    "measures": [
      {
        "name": "degree",
        "caption": "得分",
        "dataType": "NUMERIC"
      }
    ],
    "dataSourceId":"fc8dfd07-6dfb-4fe3-8937-1c1be9c69e1d"
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

```
{
  "status": 5000,
  "message": "错误",
  "moreInfo": "已经存在名为test的Cube"
}
```
## 列出所有cube

```
GET /api/cubes?pageNo=&pageSize=
```

Paramters
- query 查询串
- pageNo  页码
- pageSize 分页大小

```
{
  "status": 200,
  "message": "OK",
  "data": {
    "totalCount":10,
    "cubes":[
       {
        "cubeId": "1416ef1f-31b4-4aa8-adde-c8c9c9815e35",
        "cubeName": "test",
        "createBy": "admin",
        "modifyBy": null,
        "latModifyTime": "2017-05-10 13:03:00",
        "visible": true
      }
  ]
}
```

## 获取CUBE

```
GET /api/cubes/{{id}}

```

Paramters

- id(required): cube Id

```
{
  "status": 200,
  "message": "OK",
  "data": {
   {
     "cubeName": "string",
     "schema": {
       "tableName": "string",
       "dimensions": [
         {
           "name": "string",
           "caption": "string",
           "description": "string",
           "dataType": "string"
         }
       ],
       "measures": [
         {
           "name": "string",
           "caption": "string",
           "description": "string",
           "dataType": "string"
         }
       ],
       "dataSourceId": "string"
     }
   }
}
```


## 更新cube

```
PUT /api/cubes/{{id}}

```

Paramters

- id(required): cube Id

Body

- 同创建cube

输入：

```
 {
  "cubeName": "rename",
  "schema": {
    "tableName": "test",
    "dimensions": [
      {
        "name": "sex",
        "caption": "性别",
        "dataType": "STRING"
      }
    ],
    "measures": [
      {
        "name": "degree",
        "caption": "得分",
        "dataType": "NUMERIC"
      }
    ],
    "dataSourceId":"fc8dfd07-6dfb-4fe3-8937-1c1be9c69e1d"
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


## 删除cube

```
DELETE /api/cubes/{{id}}
```

Paramters

- id(required): cube Id

```
{
  "status": 200,
  "message": "OK",
  "data": null
}
```
