# 数据源 Api

## 创建一个数据源

- mysql 
```
POST /api/datasource
```

输入： application/json

输出： application/json


Body:
- type:String
- name: String
- host: String
- port: Int
- db:   String
- user: String
- pwd:  String 

输入示例：

```
{
  "type": "mysql",// mysql | kylin
  "name": "mysqlSource",
  "host": "host",
  "port": 10043,
  "db":   "db",
  "user": "user",
  "pwd":  "pwd"
}
```

输出示例：

- 成功：

```
{
  "status": 200,
  "message": "OK",
  "data": null
}
```

- 失败:
 - status: 5000  错误
 - status：5001 名称重复
 - status: 5002 无法连接

```
{
  "status": 5001,
  "message": "invalid name",
  "moreInfo": "名称不能重复"
}
```

## 更新数据源

```
PUT /api/datasource/{{id}}
```

- Paramters

  - id(required): 数据源Id


-Body:
  - type:String
  - name: String
  - host: String
  - port: Int
  - db:   String
  - user: String
  - pwd:  String 

同创建数据源

## 获取定义的所有数据源

```
GET  /api/datasource
```

```
{
  "status": 200,
  "message": "OK",
  "data": [
    {
      "dsId": "3ce31cd3-5b9a-4699-9365-993d0a51561b",
      "name": "mysqlSource",
      "createBy": "admin",
      "modifyBy": null,
      "lastModifyTime": "2017-05-17 14:13:50"
    }
  ]
}

```

## 获取单个数据源的定义

```
GET /api/datasource/{{id}}
```

Paramters

- id(required): 数据源Id

```
{
  "status": 200,
  "message": "OK",
  "data": {
    "type": "mysql",
    "name": "mysqlSource",
    "host": "host",
    "port": 10043,
    "db":   "db",
    "user": "user",
    "pwd":  "pwd"
  }
}
```


## 列出所有的表

```
GET /api/datasource/{{id}}/tables
```

Paramters

- id(required): 数据源Id

```
{
  "status": 200,
  "message": "OK",
  "data": [
    {
      "id": "test",
      "name": "test",
      "comment": ""
    },
    {
      "id": "log",
      "name": "log",
      "comment": ""
    },
    ...
    ]
}
```

## 列出所有字段

```
GET /api/datasource/{{dsid}}/tables/{{tableId}}/fields
```

Paramters

- dsid(required): 数据源Id
- tableid(required): 表Id

```
{
  "status": 200,
  "message": "OK",
  "data": [
    {
      "id": "degree",
      "name": "degree",
      "comment": "",
      "dataType": "NUMERIC"
    },
    {
      "id": "sex",
      "name": "sex",
      "comment": "",
      "dataType": "STRING"
    },
    {
      "id": "name",
      "name": "name",
      "comment": "",
      "dataType": "STRING"
    },
    {
      "id": "id",
      "name": "id",
      "comment": "",
      "dataType": "STRING"
    }
  ]
}
```

## 删除数据源

```
DELETE /api/datasource/{{id}}
```

Paramters

- id(required): 数据源Id

```
{
  "status": 200,
  "message": "OK",
  "data": null
}
```