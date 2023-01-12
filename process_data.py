import argparse
from datetime import datetime
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

def plot_quantiles_desyde(desyde_data):
    desyde_agg = desyde_data.groupby(['plat', ' actors']) # the space before actors is significant because of header names

    desyde_quantiles = desyde_agg.quantile(quantiles, numeric_only=True)
    desyde_min_plat = desyde_data['plat'].min()
    desyde_max_plat = desyde_data['plat'].max()
    desyde_min_actors = desyde_data[' actors'].min()
    desyde_max_actors = desyde_data[' actors'].max()
    desyde_min_runtime_in_secs = int(desyde_data[' runtime'].min() / 1000)
    desyde_max_runtime_in_secs = int(desyde_data[' runtime'].max() / 1000)

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
        ax.set_yscale('log')
        ax.set_xticks(range(desyde_min_actors, desyde_max_actors + 1))
        ax.grid(True, axis='both')
        ax.set_ylabel('Runtime [s]')
        ax.set_xlabel('\# Actors')
    # ax[-1].set_xlabel('Publication Year')
    # save the plot
    plt.tight_layout()
    fig.savefig('desyde_benchmark_plot.pdf', transparent=True, bbox_inches="tight")

def plot_quantiles_idesyde(idesyde_data):
    idesyde_agg = idesyde_data.groupby(['plat', ' actors']) # the space before actors is significant because of header names

    idesyde_quantiles = idesyde_agg.quantile(quantiles, numeric_only=True)
    idesyde_min_plat = idesyde_data['plat'].min()
    idesyde_max_plat = idesyde_data['plat'].max()
    idesyde_min_actors = idesyde_data[' actors'].min()
    idesyde_max_actors = idesyde_data[' actors'].max()
    idesyde_min_runtime_in_secs = int(idesyde_data[' runtime'].min() / 1000)
    idesyde_max_runtime_in_secs = int(idesyde_data[' runtime'].max() / 1000)

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
        ax.set_yscale('log')
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

    idesyde_data = idesyde_data[idesyde_data[' exp'] <= idesyde_data['plat'] * idesyde_data[' actors']]
    desyde_data = desyde_data[desyde_data[' exp'] <= desyde_data['plat'] * desyde_data[' actors']]

    def compute_desyde_first_sol(plat, actors, exp):
        fpath = pathlib.Path('.') / "sdfComparison" / "plat_{0}_actors_{1}".format(plat, actors) / "hsdf_{0}".format(exp) / "desyde_output" / "output.log"
        if fpath.exists():
            with open(fpath, 'r') as f:
                line = next(l for l in  f.readlines() if 'PRESOLVER executing full model - finding 2' in l)
                # datestr = line[:18]
                timestamp = datetime.fromisoformat(line[:19])
                return timestamp
        else:
            return None

        
    desyde_data[' first'] = desyde_data.apply(lambda row: compute_desyde_first_sol(row['plat'], row[' actors'], row[' exp']), axis=1)
    desyde_filtered = desyde_data.loc[desyde_data[' first'] != pd.NaT]
    desyde_filtered[' runtime_first'] = desyde_filtered.apply(lambda row: (
        row[' first'] - datetime.fromisoformat(row[' start'][:27].strip())
    ).total_seconds(), axis=1)
    desyde_filtered = desyde_filtered.loc[desyde_filtered[' first'] != pd.NaT]
    
    desyde_min_plat = desyde_filtered['plat'].min()
    desyde_max_plat = desyde_filtered['plat'].max()
    desyde_min_actors = desyde_filtered[' actors'].min()
    desyde_max_actors = desyde_filtered[' actors'].max()
    desyde_first_min_runtime_in_secs = desyde_filtered[' runtime_first'].min()
    desyde_first_max_runtime_in_secs = desyde_filtered[' runtime_first'].max()
    desyde_quantiles = desyde_filtered.groupby(['plat', ' actors']).quantile(quantiles, numeric_only=True)
    
    fig, ax = plt.subplots(1, 1, figsize=(img_width_in_inches, img_height_in_inches))
    for plat in range(desyde_min_plat, desyde_max_plat + 1):
        # for median
        series_top = desyde_quantiles.loc[plat, :, quantiles[2]]
        series_median = desyde_quantiles.loc[plat, :, quantiles[1]]
        series_bottom = desyde_quantiles.loc[plat, :, quantiles[0]]
        ax.plot(series_median.index, series_median[' runtime_first'], linestyle='--', marker='.', linewidth=0.8)
        ax.set_xlim(desyde_min_actors, desyde_max_actors)
        # ax.set_ylim(desyde_min_runtime_in_secs, desyde_max_runtime_in_secs)
        # ax.set_yticks(range(0, ymax + 1))
        ax.set_xticks(range(desyde_min_actors, desyde_max_actors + 1))
        ax.grid(True, axis='both')
        ax.set_ylabel('Runtime [s]')
        ax.set_xlabel('Number of Actors')
    # ax[-1].set_xlabel('Publication Year')
    # save the plot
    plt.tight_layout()
    fig.savefig('desyde_benchmark_first_plot.pdf', transparent=True, bbox_inches="tight")
    
    idesyde_min_plat = idesyde_data['plat'].min()
    idesyde_max_plat = idesyde_data['plat'].max()
    idesyde_min_actors = idesyde_data[' actors'].min()
    idesyde_max_actors = idesyde_data[' actors'].max()
    idesyde_first_min_runtime_in_secs = idesyde_data[' runtime_first'].min() / 1000
    idesyde_first_max_runtime_in_secs = idesyde_data[' runtime_first'].max() / 1000
    idesyde_quantiles = idesyde_data.groupby(['plat', ' actors']).quantile(quantiles, numeric_only=True)

    # for (plat, actors, exp) in idesyde_firsts.index:
    #     output_folder = pathlib.Path('sdfComparison') / "plat_{0}_actors_{1}".format(plat, actors) / "hsdf_{0}".format(exp) / "idesyde_output"
    #     start_time = datetime.fromisoformat(idesyde_firsts.loc[plat, actors, exp].strip()[:26])
    #     first_solution = min(output_folder.glob("solution*"), key=lambda f: f.stat().st_mtime)
    #     first_found = datetime.fromtimestamp(first_solution.stat().st_mtime)
    #     duration = first_found - start_time
    #     print(duration)

    fig, ax = plt.subplots(1, 1, figsize=(img_width_in_inches, img_height_in_inches))
    for plat in range(idesyde_min_plat, idesyde_max_plat + 1):
        # for median
        series_top = idesyde_quantiles.loc[plat, :, quantiles[2]]
        series_median = idesyde_quantiles.loc[plat, :, quantiles[1]]
        series_bottom = idesyde_quantiles.loc[plat, :, quantiles[0]]
        ax.plot(series_median.index, series_median[' runtime_first'] / 1000.0, linestyle='--', marker='.', linewidth=0.8)
        ax.set_xlim(idesyde_min_actors, idesyde_max_actors)
        # ax.set_ylim(idesyde_min_runtime_in_secs, idesyde_max_runtime_in_secs)
        # ax.set_yticks(range(0, ymax + 1))
        # ax.set_yscale('log')
        ax.set_xticks(range(idesyde_min_actors, idesyde_max_actors + 1))
        ax.grid(True, axis='both')
        ax.set_ylabel('Runtime [s]')
        ax.set_xlabel('Number of Actors')
    # ax[-1].set_xlabel('Publication Year')
    # save the plot
    plt.tight_layout()
    fig.savefig('idesyde_benchmark_first_plot.pdf', transparent=True, bbox_inches="tight")


def main():
    args = parse_args()
    idesyde_data = pd.read_csv('idesyde_benchmark.csv')
    desyde_data = pd.read_csv("desyde_benchmark.csv")
    if not args.no_quantiles:
        print("-- plotting quantiles idesyde --")
        if len(idesyde_data) > 0:
            plot_quantiles_idesyde(idesyde_data)
        if len(desyde_data) > 0:
            plot_quantiles_desyde(desyde_data)
    if not args.no_firsts:
        print("-- plotting firsts --")
        plot_firsts()

if __name__ == "__main__":
    main()
