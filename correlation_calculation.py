import pandas as pd
from scipy.stats import pearsonr, spearmanr, kendalltau

# Paths to your CSV files
csv_files = {
    "checkstyle": "outputs_csv/checkstyle.csv",
    "hadoop": "outputs_csv/hadoop.csv"
}

# Define a function to calculate correlations coefficients (and P-values)
def calc_correlations_coefficients(data, metric, revisions):
    # Pearson
    pearson_correlation, pearson_p = pearsonr(data[metric], data[revisions])
    # Spearman
    spearman_correlation, spearman_p = spearmanr(data[metric], data[revisions])
    # Kendall
    kendall_correlation, kendall_p = kendalltau(data[metric], data[revisions])
    
    return {
        "Pearson": (pearson_correlation, pearson_p),
        "Spearman": (spearman_correlation, spearman_p),
        "Kendall": (kendall_correlation, kendall_p)
    }

# 3 Metrics to compare from CSV
metrics = ["Size", "McCabe", "Readability"]

# Loop through each CSV file, calculate, and print correlations for each project
for project_name, file_path in csv_files.items():
    # Load the CSV file
    data = pd.read_csv(file_path)
    
    print(f"\nCorrelations coefficients for {project_name}:")
    # Calculate and print correlations for each metric
    for metric in metrics:
        correlations = calc_correlations_coefficients(data, metric, "Revisions")
        print(f"  {metric} vs. Revisions:")
        for corr_type, values in correlations.items():
            print(f"    {corr_type}: Coefficient={values[0]:.4f}, P-value={values[1]:.3e}")
    print()
