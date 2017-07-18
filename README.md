biplatform
-----------

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/06eb6fd6cfa948b3b7a2480b154c5cf6)](https://www.codacy.com/app/souo/biplatform?utm_source=github.com&utm_medium=referral&utm_content=souo/biplatform&utm_campaign=badger)
[![Build Status](https://travis-ci.org/souo/biplatform.svg?branch=master)](https://travis-ci.org/souo/biplatform)

基于scala 、akka实现了一个简单的报表工具。该项目是个玩具项目，用于个人学习scala和akka。

核心功能
=======
* 使用scala语言开发，基于akka-http, akka-stream,akka-cluster等新技术构建。
* 支持多核并发，异步无阻赛 (akka)
* 每个用户、每张报表均作为独立的actor， 通过akka Persistence持久化各个节点的状态,并额外获得一个可自动更新的分布式缓存。
* 对长时间未使用的节点 可以自动下线，以释放系统资源
* 高可用，可横向扩展至多节点。完全去中心化，无单点故障。失败可异地恢复 (akka-cluster)。
* 使用kryo序列化消息
* 流控 Back-Pressure, 避免OutOfMemory(akka-stream).

接口文档
========
[戳这里查看接口文档](doc/api.md)


system designer
===============

![designer](doc/designer.png)


Build and Run
============

* 运行`sbt clean zip` 或者 `sbt clean tgz` 在 `target/universal`目录下会生成 相应的zip或tgz包。

* 复制到服务器上 解压， 在安装目录下 运行 `./bin/run.sh` 启动服务






