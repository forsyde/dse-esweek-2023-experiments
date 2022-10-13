import pandas as pd
import matplotlib.pyplot as plt
import matplotlib as mpl

### Baseline
img_ratio = 0.7 # golden ratio - 1 is 0.618033988749894
expected_space_per_y_count = 0.3
img_width_in_inches = 3.5 # column size of a IEEE 2-column paper
img_height_in_inches = img_width_in_inches * img_ratio
quantiles = [0.2, 0.5, 0.8]

## global style configuration
mpl.rcParams.update({
    "text.usetex": True,
    "font.family": "Computer Modern Roman",
    'font.size': 10
})

idesyde_data = pd.read_csv('idesyde_benchmark.csv')
desyde_data = pd.read_csv("desyde_benchmark.csv")

desyde_agg = desyde_data.groupby(['plat', ' actors']) # the space before actors is significant because of header names
idesyde_agg = idesyde_data.groupby(['plat', ' actors']) # the space before actors is significant because of header names

desyde_quantiles = desyde_agg.quantile(quantiles, numeric_only=True)
desyde_min_plat = desyde_data['plat'].min()
desyde_max_plat = desyde_data['plat'].max()
desyde_min_actors = desyde_data[' actors'].min()
desyde_max_actors = desyde_data[' actors'].max()
desyde_min_runtime_in_secs = desyde_data[' runtime'].min() / 1000
desyde_max_runtime_in_secs = desyde_data[' runtime'].max() / 1000

idesyde_quantiles = idesyde_agg.quantile(quantiles, numeric_only=True)
idesyde_min_plat = idesyde_data['plat'].min()
idesyde_max_plat = idesyde_data['plat'].max()
idesyde_min_actors = idesyde_data[' actors'].min()
idesyde_max_actors = idesyde_data[' actors'].max()
idesyde_min_runtime_in_secs = idesyde_data[' runtime'].min() / 1000
idesyde_max_runtime_in_secs = idesyde_data[' runtime'].max() / 1000


# print(desyde_agg.quantile([0.2, 0.5, 0.8]))
# print(idesyde_agg.quantile([0.2, 0.5, 0.8]))

# first for desyde
fig, ax = plt.subplots(1, 1, figsize=(img_width_in_inches, img_height_in_inches))
for plat in range(desyde_min_plat, desyde_max_plat + 1):
    # for median
    series_top = desyde_quantiles.loc[plat, :, quantiles[2]]
    series_median = desyde_quantiles.loc[plat, :, quantiles[1]]
    series_bottom = desyde_quantiles.loc[plat, :, quantiles[0]]
    ax.plot(series_median.index, series_median[' runtime'] / 1000.0, linestyle='--', marker='.', linewidth=0.8)
    ax.set_xlim(desyde_min_actors, desyde_max_actors)
    ax.set_ylim(desyde_min_runtime_in_secs, desyde_max_runtime_in_secs)
    # ax.set_yticks(range(0, ymax + 1))
    ax.set_xticks(range(desyde_min_actors, desyde_max_actors + 1))
    ax.grid(True, axis='y')
    ax.set_ylabel('Runtime [s]')
    ax.set_xlabel('\# Actors')
# ax[-1].set_xlabel('Publication Year')
# save the plot
plt.tight_layout()
fig.savefig('desyde_benchmark_plot.pdf', transparent=True, bbox_inches="tight")

# first for idesyde
fig, ax = plt.subplots(1, 1, figsize=(img_width_in_inches, img_height_in_inches))
for plat in range(idesyde_min_plat, idesyde_max_plat + 1):
    # for median
    series_top = idesyde_quantiles.loc[plat, :, quantiles[2]]
    series_median = idesyde_quantiles.loc[plat, :, quantiles[1]]
    series_bottom = idesyde_quantiles.loc[plat, :, quantiles[0]]
    ax.plot(series_median.index, series_median[' runtime'] / 1000.0, linestyle='--', marker='.', linewidth=0.8)
    ax.set_xlim(idesyde_min_actors, idesyde_max_actors)
    ax.set_ylim(idesyde_min_runtime_in_secs, idesyde_max_runtime_in_secs)
    # ax.set_yticks(range(0, ymax + 1))
    ax.set_xticks(range(idesyde_min_actors, idesyde_max_actors + 1))
    ax.grid(True, axis='y')
    ax.set_ylabel('Runtime [s]')
    ax.set_xlabel('Number of Actors')
# ax[-1].set_xlabel('Publication Year')
# save the plot
plt.tight_layout()
fig.savefig('idesyde_benchmark_plot.pdf', transparent=True, bbox_inches="tight")
