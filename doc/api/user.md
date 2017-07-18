# user api

## 用户登入

```
POST /api/users/login
```

Headers
```
Content-Type:application/json
```

Body

- login:String  登录用户名

输入：
```
{
    "login":"admin"
}
```


返回结果：

- Body:
```
{
  "status": 200,
  "message": "OK",
  "data": {
    "isAdmin": true
  }
}
```

- Headers

```
Content-Type: application/json
Set-Cookie: _sessiondata=DACDEBAF6F612FD3FDF17C8889AEF030171CCA79-7075616AF00EF9AC204B34F6C2E64B1F18B30780A7652BB5508C285824B04961; Path=/; HttpOnly
```

## 用户登出

```
GET /api/users/logout
```

输入
- Headers
```
Cookie: _sessiondata=DACDEBAF6F612FD3FDF17C8889AEF030171CCA79-7075616AF00EF9AC204B34F6C2E64B1F18B30780A7652BB5508C285824B04961; Path=/; HttpOnly
```

输出
```
{
  "status": 200,
  "message": "OK",
  "data": null
}
```