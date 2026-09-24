/**
 * Public entry point for the shared CSV tax processing library.
 *
 * <p>Client applications should use {@link com.company.taxlibrary.TaxProcessor}
 * and its fluent builder. The facade accepts CSV data from files, paths,
 * streams, readers, or strings and returns immutable
 * {@link com.company.taxlibrary.model.TaxSummaryReport} values. Configure
 * source-specific column names with
 * {@link com.company.taxlibrary.config.MetadataConfig}.</p>
 *
 * <p>Processing is thread-safe: processor configuration is immutable, result
 * DTOs are immutable, and each invocation owns only its local parsing state.
 * File and stream overloads manage resources with try-with-resources. Reader
 * overloads process the supplied reader without taking ownership of it.</p>
 *
 * <p>Financial calculations use {@code BigDecimal}. VAT rates accept values
 * such as {@code 10}, {@code 0.1}, and {@code 10%}; monetary values are
 * rounded to scale 2 with {@code RoundingMode.HALF_UP}. Lenient processing
 * returns validation warnings in the report, while strict processing throws a
 * client-visible processing exception.</p>
 *
 * <p>Generated CSV text is sanitized against spreadsheet formula injection by
 * prefixing dangerous leading characters with an apostrophe. Internal parser
 * and writer implementation classes are not part of the recommended client
 * integration surface.</p>
 */
package com.company.taxlibrary;
