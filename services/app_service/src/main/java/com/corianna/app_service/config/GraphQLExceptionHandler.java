package com.corianna.app_service.config;

import java.util.Map;

import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.stereotype.Component;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;

@Component
public class GraphQLExceptionHandler extends DataFetcherExceptionResolverAdapter {
    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof IllegalArgumentException) {
            return GraphqlErrorBuilder.newError(env)
                    .message(ex.getMessage())
                    .errorType(org.springframework.graphql.execution.ErrorType.BAD_REQUEST)
                    .extensions(Map.of(
                            "statusCode", 400,
                            "error", "Bad Request"))
                    .build();
        }

        // fallback to default handling
        return null;
    }

}
