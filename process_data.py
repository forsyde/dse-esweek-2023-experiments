import argparse
from datetime import datetime
import pathlib
import os

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import matplotlib as mpl

### Baseline
img_ratio = 0.8 # golden ratio - 1 is 0.618033988749894
expected_space_per_y_count = 0.3
img_width_in_inches = 3.5 # column size of a IEEE 2-column paper
img_height_in_inches = img_width_in_inches * img_ratio
quantiles_bounds = [0.2, 0.5, 0.8]
marker_list = ['o', 's']

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

def plot_quantiles(total_data: pd.DataFrame, plot_name="total_runtime_benchmark"):
    total_agg = total_data.groupby(['plat', ' actors']) # the space before actors is significant because of header names

    total_quantiles = total_agg.quantile(quantiles_bounds, numeric_only=True)
    total_max = total_agg.max(numeric_only=True)
    total_min_plat = total_data['plat'].min()
    total_max_plat = total_data['plat'].max()
    total_min_actors = total_data[' actors'].min()
    total_max_actors = total_data[' actors'].max()
    total_min_runtime_in_secs = int(total_data[' runtime'].min() / 1000)
    total_max_runtime_in_secs = int(total_data[' runtime'].max() / 1000)
    num_actors = total_max_actors - total_min_actors
    num_plat = total_max_plat - total_min_plat

    fig, ax = plt.subplots(1, 1, figsize=(img_width_in_inches, img_height_in_inches))
    for (i, plat) in enumerate(range(total_min_plat, total_max_plat + 1)):
        series_max = total_max.loc[plat, :]
        # total_for_plat = total_data[total_data['plat'].eq(plat)]
        # colors = [mpl.colormaps['inferno'](1.0 - float(i)/num_plat) for _ in total_for_plat[' actors']]
        # ax.scatter(total_for_plat[' actors'] - 0.5 + float(i)/num_plat, total_for_plat[' runtime'] / 1000.0, s=1.0, c=colors, linestyle='--', marker="o", alpha=0.5, label="plat = {0}".format(i))
        ax.plot(series_max.index, series_max[' runtime'] / 1000.0, linestyle='--', lw=0.8, color=mpl.colormaps['viridis'](1.0 - float(i)/num_plat), label="\#P = {0}".format(plat), marker=".")
        ax.set_xlim(total_min_actors - 0.5, total_max_actors + 0.5)
        # ax.set_ylim(total_min_runtime_in_secs, total_max_runtime_in_secs)
        # ax.set_yticks(range(0, ymax + 1))
        ax.set_yscale('log')
        ax.set_xticks(range(total_min_actors, total_max_actors + 1))
        ax.grid(True, axis='both', linewidth=0.5)
        ax.set_ylabel('Runtime [s]')
        ax.set_xlabel('Number of actors')
    # ax[-1].set_xlabel('Publication Year')
    # save the plot
    # put a 1 day line
    if total_max_runtime_in_secs >= 60 * 60 * 24:
        ax.hlines(y=60 * 60 * 24, xmin=total_min_actors - 0.5, xmax=total_max_actors + 0.5, linestyles="dashed", colors="red", lw=0.5)
        ax.text(num_actors / 2 + 0.5, 60 * 60 * 24 - 58000, "1 Day", color="red")
    # put a 5 days line
    if total_max_runtime_in_secs >= 60 * 60 * 24 * 5:
        ax.hlines(y=60 * 60 * 24 * 5, xmin=total_min_actors - 0.5, xmax=total_max_actors + 0.5, linestyles="dashed", colors="red", lw=0.5)
        ax.text(num_actors / 2 + 0.5, 60 * 60 * 24 * 5 - 280000, "5 Days", color="red")
    ax.legend(fontsize=7)
    plt.tight_layout()
    fig.savefig(plot_name + '.pdf', transparent=True, bbox_inches="tight")
    fig.savefig(plot_name + '.png', bbox_inches="tight")

# ---------------------- FIRST SOLUTIONS ----
def plot_firsts(firsts_data: pd.DataFrame, plot_name = "first_runtime_benchmark"):
    min_plat = firsts_data['plat'].min()
    max_plat = firsts_data['plat'].max()
    min_actors = firsts_data[' actors'].min()
    max_actors = firsts_data[' actors'].max()
    first_min_runtime_in_secs = firsts_data[' runtime_first'].min()
    first_max_runtime_in_secs = firsts_data[' runtime_first'].max()
    first_agg = firsts_data.groupby(['plat', ' actors'])
    first_avg = first_agg.mean(numeric_only=True)
    
    fig, ax = plt.subplots(1, 1, figsize=(img_width_in_inches, img_height_in_inches))
    for plat in range(min_plat, max_plat + 1):
        # for median
        series_avg = first_avg.loc[plat, :]
        ax.plot(series_avg.index, series_avg[' runtime_first'], linestyle='--', marker='.', linewidth=0.8)
        ax.set_xlim(min_actors, max_actors)
        # ax.set_ylim(desyde_min_runtime_in_secs, desyde_max_runtime_in_secs)
        # ax.set_yticks(range(0, ymax + 1))
        ax.set_xticks(range(min_actors, max_actors + 1))
        ax.grid(True, axis='both')
        ax.set_ylabel('Runtime [ms]')
        ax.set_xlabel('Number of actors')
    # ax[-1].set_xlabel('Publication Year')
    # save the plot
    plt.tight_layout()
    fig.savefig(plot_name + '.pdf', transparent=True, bbox_inches="tight")
    fig.savefig(plot_name + '.png', bbox_inches="tight")

def plot_complexity_barriers(complex_data: pd.DataFrame, plot_name="complexity_barrier"):
    min_plat = complex_data['plat'].min()
    max_plat = complex_data['plat'].max()
    min_actors = complex_data[' actors'].min()
    max_actors = complex_data[' actors'].max()
    first_min_runtime_in_secs = complex_data[' runtime_first'].min()
    first_max_runtime_in_secs = complex_data[' runtime_first'].max()
    max_agg = complex_data.groupby(['plat', ' actors']).max()
    min_agg = complex_data.groupby(['plat', ' actors']).min()

    fig, ax = plt.subplots(1, 1, figsize=(img_width_in_inches, img_height_in_inches))
    zvalues = max_agg.reset_index().pivot(index='plat', columns=' actors', values=' runtime')
    xvalues, yvalues = np.meshgrid(zvalues.columns.values, zvalues.index.values)
    cs = ax.contourf(xvalues, yvalues, zvalues.values / 1000.0, [60, 3600, 3600*8, 3600*24, 3600*24*5], locator=mpl.ticker.LogLocator())
    cs2 = ax.contour(cs, colors='r')
    # ax.clabel(cs2, levels=cs.levels, inline=True, fontsize=6, fmt={k: v for (k ,v) in zip(cs.levels, ['1 min', '1 hour', '8 hours', '1 day', '5 days'])})
    cbar = fig.colorbar(cs)
    cbar.add_lines(cs2)
    cbar.set_ticklabels(['1 min', '1 hour', '8 hours', '1 day', '5 days'])
    cbar.set_label("runtime [s]")
    ax.set_ylabel('Number of cores')
    ax.set_xlabel('Number of actors')
    ax.set_xticks(range(2, 14, 2))
    ax.grid(True, axis='both', linewidth=0.3)
    plt.tight_layout()
    fig.savefig(plot_name + '.pdf', transparent=True, bbox_inches="tight")
    fig.savefig(plot_name + '.png', bbox_inches="tight")


def plot_3dcolormap(data3d: pd.DataFrame, plot_name="3d_plot"):
    min_plat = data3d['plat'].min()
    max_plat = data3d['plat'].max()
    min_actors = data3d[' actors'].min()
    max_actors = data3d[' actors'].max()
    first_min_runtime_in_secs = data3d[' runtime_first'].min()
    first_max_runtime_in_secs = data3d[' runtime_first'].max()
    max_agg = data3d.groupby(['plat', ' actors']).max()
    min_agg = data3d.groupby(['plat', ' actors']).min()

    fig, ax = plt.subplots(1, 1, figsize=(img_width_in_inches, img_height_in_inches), subplot_kw={"projection": "3d"})
    zvalues = max_agg.reset_index().pivot(index='plat', columns=' actors', values=' runtime')
    xvalues, yvalues = np.meshgrid(zvalues.columns.values, zvalues.index.values)
    surf = ax.plot_surface(xvalues, yvalues, np.log10(zvalues.values / 1000.0))
    # ax.clabel(surf, levels=surf.levels, inline=True, fontsize=6, fmt={k: v for (k ,v) in zip(surf.levels, ['1 min', '1 hour', '8 hours', '1 day'])})
    ax.set_ylabel('Number of cores')
    ax.set_xlabel('Number of actors')
    ax.set_zlabel('$\log_{{10}}$runtime')
    ax.grid(True, axis='both', linewidth=0.3)
    plt.tight_layout()
    fig.savefig(plot_name + '.pdf', transparent=True, bbox_inches="tight")
    fig.savefig(plot_name + '.png', bbox_inches="tight")


def main():
    args = parse_args()
    idesyde_data = pd.read_csv('idesyde_benchmark.csv')
    desyde_data = pd.read_csv("desyde_benchmark.csv")
    if not args.no_quantiles:
        print("-- plotting quantiles --")
        if len(idesyde_data) > 0:
            plot_quantiles(idesyde_data, "idesyde_total_benchmark")
        if len(desyde_data) > 0:
            plot_quantiles(desyde_data, "desyde_total_benchmark")
    if not args.no_firsts:
        print("-- plotting firsts --")
        if len(idesyde_data) > 0:
            plot_firsts(idesyde_data, "idesyde_firsts_benchmark")
        if len(desyde_data) > 0:
            plot_firsts(desyde_data, "desyde_firsts_benchmark")
    print("-- plotting complexity map --")
    if len(idesyde_data) > 0:
        plot_complexity_barriers(idesyde_data, "idesyde_total_complexity")
    if len(desyde_data) > 0:
        plot_complexity_barriers(desyde_data, "desyde_total_complexity")
    # print("-- plotting colormap for complexity --")
    # if len(idesyde_data) > 0:
    #     plot_3dcolormap(idesyde_data, "idesyde_total_complexity_3d")
    # if len(desyde_data) > 0:
    #     plot_3dcolormap(desyde_data, "desyde_total_complexity_3d")

if __name__ == "__main__":
    main()
