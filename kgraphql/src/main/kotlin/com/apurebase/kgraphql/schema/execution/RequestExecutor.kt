package com.apurebase.kgraphql.schema.execution

import com.apurebase.kgraphql.Context
import com.apurebase.kgraphql.request.VariablesJson
import kotlinx.coroutines.runBlocking


interface RequestExecutor {
    fun execute(plan : ExecutionPlan, variables: VariablesJson, context: Context): String = runBlocking {
        suspendExecute(plan, variables, context)
    }
    suspend fun suspendExecute(plan : ExecutionPlan, variables: VariablesJson, context: Context): String
}
