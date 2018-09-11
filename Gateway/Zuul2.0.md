# Why should msa need asyncrous, non-blocking and multi thread?
 - 이런걸 기대했더니 : 복원력(resiliency) - 대기시간(latency) 측면에서 성능 향상, 처리량(throughput), 그리고 비용 개선을 해주기를 기대했다... 그러나.
 - Zuul1은 기본적으로 Servlet Framework 기반으로 만들어져 있죠. 이런 시스셈은 기본적으로 Multithread에 Blocking시스템입니다. 즉, 하나의 요청이 들어오면 하나의 Thread를 이용해서 이러한 처리를 한다는 거죠 . 
# 대상
- Netty를 전혀 모르는 사람

# 목표
- Zuul2 적용

# 기본 지식
### Zuul2 Netty Handler 메커니즘
- 다음은 Zuul2가 HTTP request를 백엔드 서비스로 포워딩 하기 전에 실행되는 Zuul2 내 Netty Handler 
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
## 케이스1 - HTTP File Uploader/Downloader


## 케이스2 - Websocket Push Service

# 참고자료:

- Blocking-NonBlocking-Synchronous-Asynchronous: https://homoefficio.github.io/2017/02/19/Blocking-NonBlocking-Synchronous-Asynchronous/
- https://medium.com/netflix-techblog/zuul-2-the-netflix-journey-to-asynchronous-non-blocking-systems-45947377fb5c
