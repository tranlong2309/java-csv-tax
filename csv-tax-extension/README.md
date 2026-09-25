# CSV Tax Extension

This module (`com.company.taxlibrary`) utilizes the `csv-processing-core` to compute Value Added Tax (VAT) and generate financial summaries from CSV files.

## Features
- Calculates line-item VAT based on configured VAT rates.
- Generates `TaxSummaryReport` with grand totals.
- Re-exports enriched CSVs containing the original data plus calculated taxes.
