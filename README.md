# seckill
SpringBoot-Seckill


1.用户登录
  用户输入的密码两次MD5计算后入库，
    第一次：表单密码=MD5(明文密码+salt)，固定盐值，前端js脚本计算。
    第二次：数据库密码=MD5(表单密码+salt)，随机盐值存在user数据库中，后端计算。
    
2.JSR303参数校验
  自定义@IsMobile注解，IsMobileValidator校验是否为手机号码。
  
3.GlobalExceptionHandler
   @ControllerAdvice 当将异常抛到controller时,可以对异常进行统一处理,规定返回的json格式 或 跳转到一个错误页面
   @ExceptionHandler(value = Exception.class) 这个注解用指定这个方法对何种异常处理（这里默认所有异常都用这个方法处理）
   全局异常处理器接管了所有的异常处理，无论何种异常全部抛给自定义的全局异常处理器去解决。
   用户登录, 要么处理成功返回true，否则会抛出全局异常，抛出的异常信息会被全局异常接收，全局异常会将异常信息传递到全局异常处理器，自定义的GlobalExceptionHandler默认会对所有的异常都进行处理。
   
4.Redis
    FastJson将对象序列化成json字符串，自己封装jedis一个redisService，同时根据不同的数据加一个不同的prefix，以实现key的差异化。实现了redis数据的存入、取出、过期时间的设置，不使用SpringBoot提供的RedisTemplate。
    
5.分布式session
    在用户登录成功之后，后台生成一个类似JSEESSIONID的token来表示英语，通过response.addCookie通知客户端保存此cookie，浏览器的每次请求都在请求头中携带此token，然后后端根据token查询redis中是否有对应的user，由此实现了原生Session的功能。也减轻了数据库的压力。
    
6.UserArgumentResolver (注册在WebMvcConfigurer)
  当请求参数为SeckillUser时，使用这个解析器处理
     * 客户端的请求到达某个Controller的方法时，判断这个方法入参是否为SeckillUser，
     * 如果是，则这个SeckillUser参数对象通过下面的resolveArgument()方法获取。
     * 然后，该Controller方法继续往下执行时所看到的SeckillUser对象就是在这里的resolveArgument()方法处理过的对象。
     * 本来根据Cookie从Redis里获取User是在controller方法中做，现在将其封装为UserArgumentResolver，直接将user封装到在controller的入参中。
     * 且每次调用userService.getSeckillUserByToken()封装user时都同时更新cookie以延长session的有效期，这与原生seesion的超时销毁机制相同。
     
7.数据库设计
     * seckill_goods
     * seckill_order
     * seckill_user
     * order_info
     数据库连接池使用Druid，因为可以配置servlet实现sql监控。
     
8.页面设计
     * goodsList.html
     * goodDetail.html
     * ordeDetail.html
     使用模板引擎Thymeleaf、Bootstrap、Jquery
     
9.页面优化
     * 页面级缓存+URL缓存+对象缓存
     * 页面静态化、前后端分离
     * 静态资源优化
     * CDN优化
     
     页面级缓存+URL缓存+对象缓存
     
     所谓页面缓存，指的是对于服务端的请求，不直接从系统中获取页面资源，而是先从redis缓存中获取页面资源，如果缓存中不存在页面资源，则系统将渲染页面并存储页面到缓存中，然后将页面返回。适合数据基本无变化的页面。
     请求到服务端，服务端查询数据库中的商品列表信息然后存储在Model对象中，Thymeleaf页面获取在Model对象中的商品列表信息然后动态渲染，再返回给客户端。如果每次请求都做这样的工作，势必会对服务器和系统造成一定的压力（系统的压力主要来源于每次Thymeleaf页面获取在Model对象的信息都要渲染一次），所以可以做一个页面级的缓存，减轻数据库和系统的压力。
     比如商品列表页：
       @RequestMapping(value = "/toList", produces = "text/html")
       从redis取出html缓存
       html缓存不为空，返回该html
       若html为空，查询秒杀商品列表，用于手动渲染时将商品数据填充到页面
       手动渲染页面 html = thymeleafViewResolver.getTemplateEngine().process("goodsList", webContext)，并返回html。
       
     所谓URL缓存，实际上和页面缓存是一样的，在本项目中，我们对商品详情页做了缓存，商品详情页的请求需要goodsId，也就是说，对每一个goodsId对应的goodsList都做了一个缓存，其他的和商品列表页的缓存思路是一致的，只不过商品取详情页是需要动态的根据goodsId来取。
     
     通过上面的叙述可知，URL缓存和页面缓存的不同之处在于，URL缓存需要根据URL中的参数动态地取缓存，而页面缓存则不需要。
     URL缓存和页面缓存的缓存时间都比较短，在本项目中，我们设置商品详情页和商品列表页的缓存时间为60s。
       
     对象缓存是一种更细粒度的缓存，顾名思义就是对对象就行缓存，在本项目中，com.dapeng.seckill.service.SeckillUserService#getSeckillUserById和com.dapeng.seckill.service.SeckillUserService#getSeckillUserByToken的user对象都进行了缓存，但前者相当于数据库，用于登录时检验用户。而后者实际上是session。对于com.dapeng.seckill.service.SeckillUserService#updateSeckillUser方法中也缓存了对象，不过将更改的user先从redis中取出，然后删除对应的数据，然后在将新的数据更新到数据库中，再将数据缓存到redis中（getSeckillUserById）。
     为什么要先删除缓存在写入缓存呢？因为如果不删除，以前的请求仍然可以访问通过原来的token访问到以前的数据，除了造成数据的不一致还会有安全问题，所以需要删除以前的缓存在写入新的缓存，因为我们把getSeckillUserById缓存的user当数据库user用，必须保持一致性。
        更新缓存(token和user都要更新，先删除再添加)
           // 删除对象缓存（查询对象时再添加，好比实际中修改密码后重新登陆）
        redisService.delete(SeckillUserKeyPrefix.seckillUserKeyPrefixById,""+id);
          // 设置新的token缓存
        user.setPassword(toBeUpdateedUser.getPassword());
        redisService.set(SeckillUserKeyPrefix.token,token,user);// 为新的用户数据重新生成缓存（token不变只是覆盖掉了）
        
     如何保证redis和数据库数据一致性？
        更新的时候，先删除缓存，然后再更新数据库。
        读的时候，先读缓存；如果没有的话，就读数据库，同时将数据放入缓存，并返回响应。
      
     * 页面静态化、前后端分离(GET请求是幂等的，)
       页面静态化指的是将页面直接缓存到客户端。常用的技术有Angular.js，Vue.js。
       其实现方式就是通过ajax异步请求服务器获取动态数据，对于非动态数据部分缓存在客户端，客户端通过获取服务端返回的json数据解析完成相应的逻辑。在本项目中，我们对商品详情页和订单详情页做了一个静态化处理。对于商品详情页，异步地从服务端获取商品详情信息，然后客户端完成页面渲染工作。除此之外，对于秒杀信息的获取也是通过异步获取完成的。例如，当秒杀开始时，用户执行秒杀动作，客户端就会轮询服务器获取秒杀结果。而不需要服务器直接返回页面。而对于订单详情页，实际上也是同样的思路。

10.静态资源优化
     * S/CSS压缩，减少流量。客户端完成解压工作。
     * 多个JS/CSS组合，减少连接数。一次TCP连接完成多个HTTP交互。
     * CDN就近访问。
     
     CDN的全称是Content Delivery Network，即内容分发网络。CDN是构建在网络之上的内容分发网络，依靠部署在各地的边缘服务器，通过中心平台的负载均衡、内容分发、调度等功能模块，使用户就近获取所需内容，降低网络拥塞，提高用户访问响应速度和命中率。CDN的关键技术主要有内容存储和分发技术。
     
     
11.秒杀接口的实现及其优化
    1.系统加载时执行afterPropertiesSet()，从数据库查出商品库存并存入redis，同时利用HashMap内存标记库存不为空。
    2.在SeckillController.doSeckill()方法中，首先查看内存标记，减少对redis的访问，若无库存直接结束，若秒杀未结束才继续访问redis
    3.利用decrStock = redisService.decr()预减库存，并根据利用decrStock更新内存标记。
    4.再判断是否已秒杀到，防止超卖。OrderService.createOrder()是把订单信息SeckillOrder同时插入了mysql和redis，但此处查的时候只从redis查。
          这里有个问题，在redis预减库存，入队，但实际队列中的秒杀可能失败，mysql还有库存时，而redis已经没有了，出现错误！
          比如队列里10个请求，两个是同一人的，肯定有一个请求失败，但此时redis中已经减去了这个，mysql并没有减去！
      若已秒杀到，同过redisService.incr()回滚库存数据并更新内存标记。
    5.异步下单，将SeckillMessage信息（userId和goodsId）压入RabbitMQ，同时在MQListner监听队列，请求出队，取出message，再判断库存和是否已经秒杀到，如果通过再OrderInfo = seckillService.seckill()，减库存(Boolean success = goodsService.reduceStock(goodsVo))和下订单(orderService.createOrder(user,goodsVo))。
    6.返回前端：排队中。客户端轮询（轮询从Redis里查createOder里存进去的订单信息，同时兼顾Boolean isOver = getGoodsOver(goodsId)以判断是卖完了没订单，还是没卖完只是在排队，还是秒杀成功），及时返回信息，以增强用户体验。
    
12.超卖问题，Redis和数据库数据一致性问问题
    To be completed...
      
    
     
     

  
  
