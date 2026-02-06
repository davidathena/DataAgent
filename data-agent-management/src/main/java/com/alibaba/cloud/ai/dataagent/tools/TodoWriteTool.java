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
package com.alibaba.cloud.ai.dataagent.tools;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

@Slf4j
@AllArgsConstructor
@Component
public class TodoWriteTool implements BiFunction<TodoWriteTool.Request, ToolContext, String> {

  @Override
  public String apply(Request request, ToolContext toolContext) {
    log.info("========== Todo Write Tool Start ==========");

    try {
      // Validate input
      if (request.task() == null || request.task().isEmpty()) {
        return "Task description is required";
      }

      if (request.steps() == null || request.steps().isEmpty()) {
        return generateEmptyPlanOutput(request.task());
      }

      // Generate formatted output
      String output = generatePlanOutput(request.task(), request.steps());

      log.info("========== Todo Write Tool End ==========");
      return output;
    } catch (Exception e) {
      log.error("Error in todo write tool", e);
      return "Error in todo write tool: " + e.getMessage();
    }
  }

  public ToolCallback toolCallback() {
    return FunctionToolCallback.builder("todo_write", this)
        .description(
            """
                Use this tool to create and manage a structured task list for retrieval and research tasks. This helps you track progress, organize complex retrieval operations, and demonstrate thoroughness to user.

                **CRITICAL - Focus on Retrieval Tasks Only**:
                - This tool is for tracking RETRIEVAL and RESEARCH tasks (e.g., searching knowledge bases, retrieving documents, gathering information)
                - DO NOT include summary or synthesis tasks in todo_write - those are handled by thinking tool
                - Examples of appropriate tasks: "Search for X in knowledge base", "Retrieve information about Y", "Compare A and B"
                - Examples of tasks to EXCLUDE: "Summarize findings", "Generate final answer", "Synthesize results" - these are for thinking tool

                ## When to Use This Tool
                Use this tool proactively in these scenarios:

                1. Complex multi-step tasks - When a task requires 3 or more distinct steps or actions
                2. Non-trivial and complex tasks - Tasks that require careful planning or multiple operations
                3. User explicitly requests todo list - When user directly asks you to use todo list
                4. User provides multiple tasks - When users provide a list of things to be done (numbered or comma-separated)
                5. After receiving new instructions - Immediately capture user requirements as todos
                6. When you start working on a task - Mark it as in_progress BEFORE beginning work. Ideally you should only have one todo as in_progress at a time
                7. After completing a task - Mark it as completed and add any new follow-up tasks discovered during implementation

                ## When NOT to Use This Tool

                Skip using this tool when:
                1. There is only a single, straightforward task
                2. The task is trivial and tracking it provides no organizational benefit
                3. The task is purely conversational or informational

                NOTE that you should not use this tool if there is only one trivial task to do. In this case you are better off just doing task directly.

                ## Task States and Management

                1. **Task States**: Use these states to track progress:
                  - pending: Task not yet started
                  - in_progress: Currently working on (limit to ONE task at a time)
                  - completed: Task finished successfully

                2. **Task Management**:
                  - Update task status in real-time as you work
                  - Mark tasks complete IMMEDIATELY after finishing (don't batch completions)
                  - Only have ONE task in_progress at any time
                  - Complete current tasks before starting new ones
                  - Remove tasks that are no longer relevant from list entirely

                3. **Task Completion Requirements**:
                  - ONLY mark a task as completed when you have FULLY accomplished it
                  - If you encounter errors, blockers, or cannot finish, keep task as in_progress
                  - When blocked, create a new task describing what needs to be resolved
                  - Never mark a task as completed if:
                    - Tests are failing
                    - Implementation is partial
                    - You encountered unresolved errors
                    - You couldn't find necessary files or dependencies

                4. **Task Breakdown**:
                  - Create specific, actionable RETRIEVAL tasks
                  - Break complex retrieval needs into smaller, manageable steps
                  - Use clear, descriptive task names focused on what to retrieve or research
                  - **DO NOT include summary/synthesis tasks** - those are handled separately by thinking tool

                **Important**: After completing all retrieval tasks in todo_write, use thinking tool to synthesize findings and generate final answer. The todo_write tool tracks WHAT to retrieve, while thinking tool handles HOW to synthesize and present information.

                When in doubt, use this tool. Being proactive with task management demonstrates attentiveness and ensures you complete all retrieval requirements successfully.
                """)
        .inputType(Request.class)
        .build();
  }

  @JsonClassDescription("Request for todo write tool")
  public record Request(
      @JsonProperty(value = "task", required = true)
      @JsonPropertyDescription("The complex task or question you need to create a plan for")
      String task,

      @JsonProperty(value = "steps", required = true)
      @JsonPropertyDescription("Array of research plan steps with status tracking")
      List<PlanStep> steps
  ) {

  }

  @JsonClassDescription("Plan step for todo write tool")
  public record PlanStep(
      @JsonProperty(value = "id", required = true)
      @JsonPropertyDescription("Unique identifier for this step (e.g., 'step1', 'step2')")
      String id,

      @JsonProperty(value = "description", required = true)
      @JsonPropertyDescription("Clear description of what to investigate or accomplish in this step")
      String description,

      @JsonProperty(value = "status", required = true)
      @JsonPropertyDescription("Current status: pending (not started), in_progress (executing), completed (finished)")
      String status
  ) {

  }

  private String generateEmptyPlanOutput(String task) {
    StringBuilder output = new StringBuilder();
    output.append("计划已创建\n\n");
    output.append("**任务**: ").append(task).append("\n\n");
    output.append("注意：未提供具体步骤。建议创建3-7个检索任务以系统化研究。\n\n");
    output.append("建议的检索流程（专注于检索任务，不包含总结）：\n");
    output.append("1. 使用 grep_chunks 搜索关键词定位相关文档\n");
    output.append("2. 使用 knowledge_search 进行语义搜索获取相关内容\n");
    output.append("3. 使用 list_knowledge_chunks 获取关键文档的完整内容\n");
    output.append("4. 使用 web_search 获取补充信息（如需要）\n");
    output.append("\n注意：总结和综合由 thinking 工具处理，不要在此处添加总结任务。\n");
    return output.toString();
  }

  private String generatePlanOutput(String task, List<PlanStep> steps) {
    StringBuilder output = new StringBuilder();
    output.append("计划已创建\n\n");
    output.append("**任务**: ").append(task).append("\n\n");
    output.append("**计划步骤**:\n\n");

    // Count task statuses
    int pendingCount = 0;
    int inProgressCount = 0;
    int completedCount = 0;
    for (PlanStep step : steps) {
      switch (step.status()) {
        case "pending":
          pendingCount++;
          break;
        case "in_progress":
          inProgressCount++;
          break;
        case "completed":
          completedCount++;
          break;
      }
    }
    int totalCount = steps.size();
    int remainingCount = pendingCount + inProgressCount;

    // Display all steps in order
    for (int i = 0; i < steps.size(); i++) {
      output.append(formatPlanStep(i + 1, steps.get(i)));
    }

    // Add summary and emphasis on remaining tasks
    output.append("\n=== 任务进度 ===\n");
    output.append("总计: ").append(totalCount).append(" 个任务\n");
    output.append("✅ 已完成: ").append(completedCount).append(" 个\n");
    output.append("🔄 进行中: ").append(inProgressCount).append(" 个\n");
    output.append("⏳ 待处理: ").append(pendingCount).append(" 个\n");

    output.append("\n=== ⚠️ 重要提醒 ===\n");
    if (remainingCount > 0) {
      output.append("**还有 ").append(remainingCount).append(" 个任务未完成！**\n\n");
      output.append("**必须完成所有任务后才能总结或得出结论。**\n\n");
      output.append("下一步操作：\n");
      if (inProgressCount > 0) {
        output.append("- 继续完成当前进行中的任务\n");
      }
      if (pendingCount > 0) {
        output.append("- 开始处理 ").append(pendingCount).append(" 个待处理任务\n");
        output.append("- 按顺序完成每个任务，不要跳过\n");
      }
      output.append("- 完成每个任务后，更新 todo_write 标记为 completed\n");
      output.append("- 只有在所有任务完成后，才能生成最终总结\n");
    } else {
      output.append("✅ **所有任务已完成！**\n\n");
      output.append("现在可以：\n");
      output.append("- 综合所有任务的发现\n");
      output.append("- 生成完整的最终答案或报告\n");
      output.append("- 确保所有方面都已充分研究\n");
    }

    return output.toString();
  }

  private String formatPlanStep(int index, PlanStep step) {
    String statusEmoji;
    switch (step.status()) {
      case "pending":
        statusEmoji = "⏳";
        break;
      case "in_progress":
        statusEmoji = "🔄";
        break;
      case "completed":
        statusEmoji = "✅";
        break;
      default:
        statusEmoji = "⏳";
    }

    return String.format("  %d. %s [%s] %s\n", index, statusEmoji, step.status(),
        step.description());
  }

}
