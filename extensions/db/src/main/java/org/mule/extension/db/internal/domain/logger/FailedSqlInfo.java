package org.mule.extension.db.internal.domain.logger;

import java.util.List;

import org.mule.extension.db.internal.domain.query.BulkQuery;
import org.mule.extension.db.internal.domain.query.Query;
import org.mule.extension.db.internal.domain.query.QueryParamValue;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collect the Sql infos even {@link DefaultQueryLoggerFactory} did return a
 * Null*QueryLogger.
 * 
 * @since 4.0
 */
public class FailedSqlInfo {

	private static final Logger LOGGER = LoggerFactory.getLogger(FailedSqlInfo.class);

	public void logSqlCausingException(Logger logger, Query query) {
		if (logger.isDebugEnabled()) {
			LOGGER.debug("SQL should be in log already via DebugSingleQueryLogger");
			return;
		}
		try {
			StringBuilder info = new StringBuilder();
			info.append("Error SQL:");
			info.append(query.getQueryTemplate().getSqlText());
			info.append(" with ");
			for (QueryParamValue queryParamValue : query.getParamValues()) {
				info.append(queryParamValue.getName());
				info.append("=");
				info.append(queryParamValue.getValue());
				info.append(" ");
			}
			logger.error(info.toString());
		} catch (RuntimeException e) {
			LOGGER.error("Error while logging", e);
		}
	}

	/**
	 * 
	 */
	public void logSqlCausingException(Logger logger, BulkQuery bulkQuery) {
		if (logger.isDebugEnabled()) {
			LOGGER.debug("SQL should be in log already via DebugBulkQueryLogger");
			return;
		}
		try {
			StringBuilder info = new StringBuilder();
			info.append("Error SQLs:");
			List<QueryTemplate> queryTemplates = bulkQuery.getQueryTemplates();
			for (QueryTemplate queryTemplate : queryTemplates) {
				info.append(System.lineSeparator());
				info.append(queryTemplate.getSqlText());
			}
			logger.error(info.toString());
		} catch (RuntimeException e) {
			LOGGER.error("Error while logging", e);
		}
	}

	/**
	 * 
	 */
	public void logSqlCausingException(Logger logger, Query query, List<List<QueryParamValue>> bulkParamValues) {

		if (logger.isDebugEnabled()) {
			LOGGER.debug("SQL should be in log already via DebugPreparedBulkQueryLogger");
			return;
		}
		try {
			StringBuilder info = new StringBuilder();
			info.append("Error SQL:");
			info.append(System.lineSeparator());
			info.append(query.getQueryTemplate().getSqlText());
			for (List<QueryParamValue> queryParam : bulkParamValues) {
				info.append(System.lineSeparator());
				info.append(" with ");
				for (QueryParamValue queryParamValue : queryParam) {
					info.append(queryParamValue.getName());
					info.append("=");
					info.append(queryParamValue.getValue());
					info.append(" ");
				}
			}
			logger.error(info.toString());
		} catch (RuntimeException e) {
			LOGGER.error("Error while logging", e);
		}
	}
}
