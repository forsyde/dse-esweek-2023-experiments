import argparse
import datetime
import pathlib
import os

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


def parse_args():
    parser = argparse.ArgumentParser(
        prog="process_data",
        usage="Generate the required data",
        description="""A handy script to choose which parts of the results to (re)generate."""
    )
    parser.add_argument(
        "--no-quantiles",
        action="store_true",
        help="If the quantiles calculation and plotting should be skippped."
    )
    parser.add_argument(
        "--no-firsts",
        action="store_true",
        help="If the time-to-first calculation and plotting should be skippped."
    )

    return parser.parse_args()

def plot_quantiles():
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
        # ax.set_ylim(desyde_min_runtime_in_secs, desyde_max_runtime_in_secs)
        # ax.set_yticks(range(0, ymax + 1))
        ax.set_xticks(range(desyde_min_actors, desyde_max_actors + 1))
        ax.grid(True, axis='both')
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
        # ax.set_ylim(idesyde_min_runtime_in_secs, idesyde_max_runtime_in_secs)
        # ax.set_yticks(range(0, ymax + 1))
        ax.set_xticks(range(idesyde_min_actors, idesyde_max_actors + 1))
        ax.grid(True, axis='both')
        ax.set_ylabel('Runtime [s]')
        ax.set_xlabel('Number of Actors')
    # ax[-1].set_xlabel('Publication Year')
    # save the plot
    plt.tight_layout()
    fig.savefig('idesyde_benchmark_plot.pdf', transparent=True, bbox_inches="tight")


# ---------------------- FIRST SOLUTIONS ----
def plot_firsts():
    idesyde_data = pd.read_csv('idesyde_benchmark.csv')
    desyde_data = pd.read_csv("desyde_benchmark.csv")

    desyde_firsts = desyde_data.groupby(['plat', ' actors', ' exp'])[' start'].max()
    idesyde_firsts = idesyde_data.groupby(['plat', ' actors', ' exp'])[' start'].max()

    for (plat, actors, exp) in idesyde_firsts.index:
        output_folder = pathlib.Path('sdfComparison') / "plat_{0}_actors_{1}".format(plat, actors) / "hsdf_{0}".format(exp) / "idesyde_output"
        start_time = idesyde_firsts.loc[plat, actors, exp]
        first_solution = min(output_folder.glob("solution*"), key=lambda f: f.stat().st_mtime)
        first_found = datetime.datetime.fromtimestamp(first_solution.stat().st_mtime)
        duration = first_found - start_time
        print(duration)


    print(idesyde_firsts)


def main():
    args = parse_args()
    if not args.no_quantiles:
        print("-- plotting quantiles --")
        plot_quantiles()
    if not args.no_firsts:
        print("-- plotting firsts --")
        plot_firsts()

if __name__ == "__main__":
    main()
