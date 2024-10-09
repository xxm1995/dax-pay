package org.dromara.daxpay.unisdk.common.api;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dromara.daxpay.unisdk.common.bean.PayMessage;
import org.dromara.daxpay.unisdk.common.bean.PayOutMessage;
import org.dromara.daxpay.unisdk.common.util.LogExceptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * <pre>
 * 支付消息路由器，通过代码化的配置，把来自支付的消息交给handler处理
 *
 * 说明：
 * 1. 配置路由规则时要按照从细到粗的原则，否则可能消息可能会被提前处理
 * 2. 默认情况下消息只会被处理一次，除非使用 {@link PayMessageRouterRule#next()}
 * 3. 规则的结束必须用{@link PayMessageRouterRule#end()}或者{@link PayMessageRouterRule#next()}，否则不会生效
 *
 * 使用方法：
 * PayMessageRouter router = new PayMessageRouter();
 * router
 *   .rule()
 *       .msgType("MSG_TYPE").event("EVENT").eventKey("EVENT_KEY").content("CONTENT")
 *       .interceptor(interceptor, ...).handler(handler, ...)
 *   .end()
 *   .rule()
 *       // 另外一个匹配规则
 *   .end()
 * ;
 *
 * // 将PayMessage交给消息路由器
 * router.route(message);
 *  source chanjarster/weixin-java-tools  Daniel Qian
 * </pre>
 *
 * @author egan
 */
@Slf4j
public class PayMessageRouter {

    /**
     * 异步线程大小
     */
    private static final int DEFAULT_THREAD_POOL_SIZE = 100;
    /**
     * 规则集
     */
    private final List<PayMessageRouterRule> rules = new ArrayList<>();
    /**
     * 支付服务
     */
    private final UniPayService payService;
    /**
     * 异步线程处理器
     * -- SETTER --
     *  <pre>
     *  设置自定义的
     *  如果不调用用该方法，默认使 Executors.newFixedThreadPool(100)
     *  </pre>
     *
     * @param executorService 异步线程处理器

     */
    @Setter
    private ExecutorService executorService;
    /**
     * 支付异常处理器
     * -- SETTER --
     *  <pre>
     *  设置自定义的
     *  如果不调用该方法，默认使用
     *  </pre>
     *
     * @param exceptionHandler 异常处理器

     */
    @Setter
    private PayErrorExceptionHandler exceptionHandler;

    /**
     * 根据支付服务创建路由
     *
     * @param payService 支付服务
     */
    public PayMessageRouter(UniPayService payService) {
        this.payService = payService;
        this.executorService = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE);
        this.exceptionHandler = new LogExceptionHandler();
    }


    /**
     * 获取所有的规则
     *
     * @return 规则
     */
    List<PayMessageRouterRule> getRules() {
        return this.rules;
    }

    /**
     * 开始一个新的Route规则
     *
     * @return 新的Route规则
     */
    public PayMessageRouterRule rule() {
        return new PayMessageRouterRule(this);
    }

    /**
     * 处理支付消息
     *
     * @param payMessage 支付消息
     * @param storage    支付配置
     * @return 支付输出结果
     */
    public PayOutMessage route(Map<String, Object> payMessage, PayConfigStorage storage) {
        PayMessage message = payService.createMessage(payMessage);
        message.setPayType(storage.getPayType());

        return route(message);
    }

    /**
     * 处理支付消息
     *
     * @param payMessage 支付消息
     * @return 支付输出结果
     */
    public PayOutMessage route(final PayMessage payMessage) {

        final List<PayMessageRouterRule> matchRules = new ArrayList<>();
        // 收集匹配的规则
        for (final PayMessageRouterRule rule : rules) {
            if (rule.test(payMessage)) {
                matchRules.add(rule);
                if (!rule.isReEnter()) {
                    break;
                }
            }
        }

        if (matchRules.isEmpty()) {
            return null;
        }

        PayOutMessage res = null;
        final List<Future> futures = new ArrayList<>();
        for (final PayMessageRouterRule rule : matchRules) {
            // 返回最后一个非异步的rule的执行结果
            if (rule.isAsync()) {
                futures.add(
                        executorService.submit(() -> {
                            rule.service(payMessage, payService, exceptionHandler);
                        })
                );
            }
            else {
                res = rule.service(payMessage, payService, exceptionHandler);
                // 在同步操作结束，session访问结束
                if (log.isDebugEnabled()) {
                    log.debug("End session access: async=false, fromPay={}", payMessage.getFromPay());
                }
            }
        }

        if (!futures.isEmpty()) {
            executorService.submit(() -> {
                for (Future future : futures) {
                    try {
                        future.get();
                        log.debug("End session access: async=true, fromPay={}", payMessage.getFromPay());

                    }
                    catch (InterruptedException | ExecutionException e) {
                        log.error("Error happened when wait task finish", e);
                    }
                }
            });
        }
        return res;
    }


}
