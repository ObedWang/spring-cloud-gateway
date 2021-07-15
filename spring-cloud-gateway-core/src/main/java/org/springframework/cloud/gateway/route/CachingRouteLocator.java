/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.springframework.cloud.gateway.route;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import reactor.cache.CacheFlux;
import reactor.core.publisher.Flux;

import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * 缓存RouteLocator，该类增强的功能是可以refresh（监听config修改事件）<br>
 * 当refresh时，就清空缓存Route的hashMap，这个时候，再来获取Route，因为获取不到，就调用onCacheMissResume方法重新加载Route
 * @author Spencer Gibb
 */
public class CachingRouteLocator implements RouteLocator {

	private final RouteLocator delegate;
	private final Flux<Route> routes;
	private final Map<String, List> cache = new HashMap<>();

	public CachingRouteLocator(RouteLocator delegate) {
		this.delegate = delegate;
		//返回一个FluxCacheBuilderMapMiss，相当于一个reactive提供的缓存，如果没找到对应的数据，回去调用onCacheMissResume方法
		//讲道理应该是线程不安全的，但是因为是在构造方法里创建，所以应该只有一个线程调用所以是安全的
		routes = CacheFlux.lookup(cache, "routes", Route.class)
				.onCacheMissResume(() -> this.delegate.getRoutes().sort(AnnotationAwareOrderComparator.INSTANCE));
	}

	@Override
	public Flux<Route> getRoutes() {
		return this.routes;
	}

	/**
	 * Clears the routes cache
	 * @return routes flux
	 */
	public Flux<Route> refresh() {
		this.cache.clear();
		return this.routes;
	}

	/**
	 * 事件监听，用于监听RefreshRouteEvent
	 */
	@EventListener(RefreshRoutesEvent.class)
	/* for testing */ void handleRefresh() {
		refresh();
	}
}
