# 대상
- Netty를 전혀 모르는 사람

# 목표
- Zuul2 적용

# 기본 지식
케이스1. Zuul2	내부 이해
  - netty로 어떻게 짜여져 있는가
  - zuul2(Proxy) filter 동작하는지
    - 1 http ( request 기본 처리..)
Request from Browser 

HttpServerLifecycleChannelHandler
	HttpServerLifecycleInboundChannelHandler.channelRead (msg) - request

HttpClientLifecycleChannelHandler
	HttpClientLifecycleOutboundChannelHandler.write(msg) - request

ProxyEndpoint
	writeClientRequestToOrigin
		ch.flush()
——

Remote Backend Service flow

——

HttpClientLifecycleChannelHandler
	HttpClientLifecycleInboundChannelHandler.channelRead(msg) - response

ZuulResponseFilter.apply(response)


HttpServerLifecycleChannelHandler
	HttpServerLifecycleOutboundChannelHandler.write(httpResponse) - response

Response To Browser

    - 2 File
- Push 서비스
    -  3 Web socket
	
# 따라하기/유즈케이스/...



케이스2. NIO 로 백엔드 서비스 구현하기 위해서 
- 1. 서론 —> Netty 의 동작방식????

 - HTTP 통신에 대한 Handler 설명
 - 

Websocket 
    —> 바이너리 어떻게 처리하는지? 
 - Proxy 통신 —> 
