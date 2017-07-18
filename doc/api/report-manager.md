# report manager Api

### 创建报表

```
POST /api/designer/reports
```

输入： application/json

输出： application/json

Body

- name (required)


输入示例
```json
{
  "name": "report2"
}
```

输出示例

```json
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
    "moreInfo": "报表名重复"
  }
```
## 获取所有报表

```
GET /api/designer/reports?published=???&query=???&pageNo=???&pageSize=???
```

Parameters

- published:Boolean, 发布状态:true或者false,默认false
- query  包含指定字符串
- pageNo
- pageSize


返回示例:

```json
{
  "status": 200,
  "message": "OK",
  "data": {
    "totalCount": 10,
    "reports": [
    {
      "id": "484b6973-ffc8-4ecd-993e-3222ffa9f05a",
      "name": "report1",
      "createBy": "admin",
      "createTime": "2017-05-10 13:10:02",
      "lastEditTime": null,
      "lastPublishTime": null,
      "edited": false,
      "published": false
    },
    {
      "id": "be9477da-a7f8-4493-908f-61d1c1719630",
      "name": "report2",
      "createBy": "admin",
      "createTime": "2017-05-10 13:12:06",
      "lastEditTime": null,
      "lastPublishTime": null,
      "edited": false,
      "published": false
    }
  ]
  }
}
```

## 更新报表(重命名)

```
PUT /api/designer/reports
```



输入： application/json

输出： application/json

Body

- id
- name

输入示例
```json
{
      "id": "7de252b3-4634-47e1-9a3e-86ca99c38ded",
      "name": "rename report"
}
```


输出示例

```json
{
  "status": 200,
  "message": "OK",
  "data": null
}
```


## 删除报表

```
DELETE /api/designer/reports/{{reportId}}
```


Parameters

- reportId (required) 报表ID


请求示例
```
DELETE /api/designer/reports/7de252b3-4634-47e1-9a3e-86ca99c38ded
```


输出示例:
```
{
  "status": 200,
  "message": "OK",
  "data": null
}
```


