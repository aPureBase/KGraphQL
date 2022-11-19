package com.apurebase.kgraphql.schema.execution

import com.apurebase.kgraphql.Context
import com.apurebase.kgraphql.request.VariablesJson


interface RequestExecutor {
    suspend fun suspendExecute(plan: ExecutionPlan, variables: VariablesJson, context: Context): String
}
