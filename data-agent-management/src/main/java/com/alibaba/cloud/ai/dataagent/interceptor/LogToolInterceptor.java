/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.dataagent.interceptor;

import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LogToolInterceptor extends ToolInterceptor {

	private static final Logger log = LoggerFactory.getLogger(LogToolInterceptor.class);

	public interface ToolCallCallback {
		void onToolCall(String threadId, String toolName, String input, String output);
	}

	private final List<ToolCallCallback> callbacks = new ArrayList<>();

	@Override
	public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
		log.info("ToolInterceptor: Tool {} is called!", request.getToolName());
		String threadId = request.getContext().get("thread_id").toString();
		String inputs = request.getArguments();
		// 调用所有回调
		log.info(" the tool callback start, size: {}", callbacks.size());
		ToolCallResponse response = handler.call(request);
		String output = response.getResult();
		// 调用所有回调
		for (ToolCallCallback callback : callbacks) {
			try {
				callback.onToolCall(threadId, request.getToolName(), inputs, output);
			} catch (Exception e) {
				log.error("Error in tool call callback: {}", e.getMessage(), e);
			}
		}
		return response;
	}

	@Override
	public String getName() {
		return "LogToolInterceptor";
	}

	public void addToolCallCallback(ToolCallCallback callback) {
		log.info("add tool call callback: {}", callback);
		callbacks.add(callback);
	}

	public void removeToolCallCallback(ToolCallCallback callback) {
		callbacks.remove(callback);
	}

}
