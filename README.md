# UniversalGrpcClient

UnivesalGrpcClient ia a command line application.
When fed with grpc proto and request in a particular JSON format can invoke any GRPC serivce. It uses proto descriptors to build the client dynamicaly and to marshal and unmarshal the JSON request to/from protobuf message.

## Features
- It can invoke any GRPC service when providede with proto and all dependent proto files
- GRPC service can be invoked with metadata (headers)
- Miultiple services can be called hosted in different domain
- Services can be called in sequence, you can define order of the service in request JSON
- When services called in a sequence, previous service's response can be used as metadata or headers in services which comes later in order.
- Each grpc service call must provide request information particular JSON format, with some control parameter applicable to that particuar service

## Command line parameters

Parameters with  * are mandatory
- protoRootDirectory*: Where all protos resides including dependency proto files are located. It is mandatory parameter.
- protoBufDirectory*: it is where proto descriptor files will genreted and kept. Only for application internal usage.
-    protoFiles*: proto file locations from "protoRootDirectory". Multiple file location should be white space separated. 
 If protoRootDirectory is /home/demo/protodir then all proto files are under this locations including dependecies. if there are two protos /home/demo/protodir/ugcDemo1/product/product.proto and  /home/demo/protodir/ugcDemo1/user/user.proto then --protoFiles will be "/ugcDemo1/user/user.proto /ugcDemo1/product/product.proto"
- requestJsonDirectory*: Where all request JSON files are located 
- printResponse: if want to print response in logs. No value is required

## Request Json

For each GRPC call a request JSON file is required which defines behavior of the Dynamic GRPC client for that request.

The Dynamic client behavior can be controlled with parameters.
Parameters are listed below, mandatory ones are marked with *.
- endpoint.host*: Domain/IP address where GRPC server is hosted 
- endpoint.port*: GRPC server port
- endpoint.secure: If http communication is prtacted via TLS it should be "true". default value is "false".
- requestData*: grpc request body in JSON format.
- metadata*: GRPC request headers in JSON format.
- fullMethodName*: Full name of grpc method you want to invoke. in format "package.service.rpc".
- serviceCallOrder: if you want to invoke grpc methods in a sequence, request with serviceCallOrder=0 will be served before request with serviceCallOrder=1.

-ignoreThisRequest: When there are many request file in the requestJsonDirectory but you dont want to invoke all of them just provide this parameter as "true". Default value is false.

crossRequestkeyToMetadataKey: There is a crossRequestKey which shoud be unique accross all request JSON Key holds the value of response from another
