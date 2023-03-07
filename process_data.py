import argparse
from datetime import datetime
import pathlib
import os

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import matplotlib as mpl
from matplotlib.legend_handler import HandlerTuple

### Baseline
img_ratio = 0.8  # golden ratio - 1 is 0.618033988749894
expected_space_per_y_count = 0.3
img_width_in_inches = 5  # column size of a IEEE 2-column paper
img_height_in_inches = img_width_in_inches * img_ratio
quantiles_bounds = [0.2, 0.5, 0.8]
marker_list = ["o", "s"]

## global style configuration
mpl.rcParams.update(
    {"text.usetex": True, "font.family": "Computer Modern Roman", "font.size": 9}
)


def plot_firings(
    total_data: pd.DataFrame,
    xMax=None,
    zCol=" runtime",
    zColLabel="Total runtime",
    zScale=0.001,
    plot_name="total_runtime_benchmark",
    isLog=False,
    timeoutInMillis=1000 * 30 * 60,
    drawTimeoutLine=False,
    isMax=True,
    loc="best",
    xLabel="Number of firings ($\sum q_G$)"
):
    filtered_data = total_data[total_data[" firings"] <= xMax] if xMax else total_data
    total_agg = filtered_data.groupby(
        ["plat", " firings"]
    )  # the space before actors is significant because of header names
    total_reduced = total_agg.max(numeric_only=True) if isMax else total_agg.mean(numeric_only=True)
    total_min_plat = filtered_data["plat"].min()
    total_max_plat = filtered_data["plat"].max()
    # total_min_actors = filtered_data[" firings"].min()
    # total_max_actors = filtered_data[" firings"].max()
    # total_min_runtime_in_secs = int(total_data[" runtime"].min() * zScale)
    # total_max_runtime_in_secs = int(total_data[" runtime"].max() * zScale)
    # num_actors = total_max_actors - total_min_actors
    # median_actor = total_max_actors / 2 + total_min_actors / 2
    num_plat = total_max_plat - total_min_plat

    fig, ax = plt.subplots(1, 1, figsize=(img_width_in_inches, img_height_in_inches))
    handles = []
    for i, plat in enumerate(range(total_min_plat, total_max_plat + 1)):
        # use one type of marker for completions, and another for time-outs
        series_max = filtered_data[filtered_data["plat"] == plat]
        series_max_timed_out_no_sol = series_max[(series_max[" runtime"] >= timeoutInMillis) & (series_max[" nsols"] == 0)]
        series_max_timed_out = series_max[(series_max[" runtime"] >= timeoutInMillis) & (series_max[" nsols"] > 0)]
        series_max_completed = series_max[series_max[" runtime"] < timeoutInMillis]
        # if lineStyle:
        #     ax.scatter(
        #         series_max[" firings"],
        #         series_max[zCol] * zScale,
        #         linestyle=lineStyle,
        #         lw=0.8,
        #         color=mpl.colormaps["viridis"](1.0 - float(i) / num_plat),
        #         label="$|P| = {0}$".format(plat) if lineStyle else None,
        #         marker="None",
        #     )
        p1 = ax.scatter(
            series_max_completed[" firings"],
            series_max_completed[zCol] * zScale,
            color=mpl.colormaps["viridis"](1.0 - float(i) / num_plat),
            marker=".",
        )
        p2 = ax.scatter(
            series_max_timed_out[" firings"],
            series_max_timed_out[zCol] * zScale,
            12.0,
            color=mpl.colormaps["viridis"](1.0 - float(i) / num_plat),
            marker="^",
        )
        p3 = ax.scatter(
            series_max_timed_out_no_sol[" firings"],
            series_max_timed_out_no_sol[zCol] * zScale,
            12.0,
            color=mpl.colormaps["viridis"](1.0 - float(i) / num_plat),
            marker="x",
        )
        handles.append((p1, p2, p3))
        # ax.set_xlim(total_min_actors - 0.5, total_max_actors + 0.5)
        # ax.set_ylim(total_min_runtime_in_secs, total_max_runtime_in_secs)
        # ax.set_yticks(range(0, ymax + 1))
        if isLog:
            ax.set_yscale("log")
        # ax.set_xticks(range(total_min_actors, total_max_actors + 1))
        ax.grid(True, axis="both", linewidth=0.5)
        ax.set_ylabel(zColLabel)
        ax.set_xlabel(xLabel)
    if drawTimeoutLine:
        ax.axhline(y = timeoutInMillis * zScale, color='r', lw=0.4, linestyle="dashdot")
    # save the plot
    ax.legend(handles, ["$|P| = {0}$".format(plat) for plat in range(total_min_plat, total_max_plat + 1)], handler_map={tuple: HandlerTuple(ndivide=None)}, fontsize=7, loc=loc)
    plt.tight_layout()
    fig.savefig(plot_name + ".pdf", transparent=True, bbox_inches="tight")
    # fig.savefig(plot_name + ".png", bbox_inches="tight")


def plot_complexity_barriers(
    complex_data: pd.DataFrame,
    zCol=" runtime",
    zLevels=[],
    zAxisLabel="",
    zLabels=[],
    is_log=False,
    xMax=None,
    plot_name="complexity_barrier",
):
    max_agg = (
        complex_data.groupby(["plat", " firings"]).max()
        if not xMax
        else complex_data[complex_data[" firings"] <= xMax]
        .groupby(["plat", " firings"])
        .max()
    )
    min_agg = complex_data.groupby(["plat", " firings"]).min()

    fig, ax = plt.subplots(1, 1, figsize=(img_width_in_inches, img_height_in_inches))
    zvalues = max_agg.reset_index().pivot(index="plat", columns=" firings", values=zCol)
    xvalues, yvalues = np.meshgrid(zvalues.columns.values, zvalues.index.values)
    cs = None
    if zLevels and is_log:
        cs = ax.contourf(
            xvalues,
            yvalues,
            zvalues.values,
            zLevels,
            cmap="coolwarm",
            locator=mpl.ticker.LogLocator(),
        )
    elif zLevels and not is_log:
        cs = ax.contourf(xvalues, yvalues, zvalues.values, zLevels, cmap="coolwarm")
    elif not zLevels and is_log:
        cs = ax.contourf(
            xvalues,
            yvalues,
            zvalues.values,
            cmap="coolwarm",
            locator=mpl.ticker.LogLocator(),
        )
    else:
        cs = ax.contourf(xvalues, yvalues, zvalues.values, cmap="coolwarm")
    # cs2 = ax.contour(cs, colors='black')
    # ax.clabel(cs2, levels=cs.levels, fontsize=6, fmt={k: v for (k ,v) in zip(cs.levels, ['1 s', '1 min', '1 hour', '8 hours', '1 day', '5+ days'])})
    cbar = fig.colorbar(cs)
    # cbar.add_lines(cs2)
    if zLabels:
        cbar.set_ticklabels(zLabels)
    if zAxisLabel:
        cbar.set_label(zAxisLabel)
    ax.set_ylabel("Number of cores ($|P|$)")
    ax.set_xlabel("Number of firings ($\sum q_G$)")
    # if xMax:
    #     ax.set_xlim((xvalues.min(), xMax))
    # ax.set_xticks(range(2, 14, 2))
    ax.grid(True, axis="both", linewidth=0.3)
    plt.tight_layout()
    fig.savefig(plot_name + ".pdf", transparent=True, bbox_inches="tight")
    fig.savefig(plot_name + ".png", bbox_inches="tight")


def main():
    idesyde_data = pd.read_csv("idesyde_benchmark.csv")
    idesyde_scal_data = pd.read_csv("idesyde_scal_benchmark.csv")
    print("-- plotting worst case comp time --")
    if len(idesyde_data) > 0:
        plot_firings(
            idesyde_data,
            zCol=" runtime",
            zColLabel="Time to completion or time-out [s]",
            isLog=True,
            timeoutInMillis=1000 * 3600 * 24 * 5,
            drawTimeoutLine=True,
            plot_name="idesyde_total",
            loc="center left",
            xLabel="Number of actors ($|A|$)"
        )
    print("-- plotting wrost case num sols --")
    if len(idesyde_data) > 0:
        plot_firings(
            idesyde_data,
            zCol=" nsols",
            zColLabel="Number of solutions",
            zScale=1,
            xMax=150,
            plot_name="idesyde_total_nsols",
        )
    print("-- plotting complexity map --")
    if len(idesyde_data) > 0:
        plot_complexity_barriers(
            idesyde_data,
            zCol=" runtime",
            zAxisLabel="$\log_{{10}}$ runtime [s]",
            zLevels=[
                1000,
                1000 * 60,
                1000 * 1800,
                1000 * 3600,
                1000 * 3600 * 8,
                1000 * 3600 * 24,
                1000 * 3600 * 120,
            ],
            zLabels=[
                "1 sec",
                "1 min",
                "30 mins",
                "1 hour",
                "8 hours",
                "1 day",
                "5 days",
            ],
            is_log=True,
            plot_name="idesyde_total_complexity",
        )
    if len(idesyde_data) > 0:
        plot_firings(
            idesyde_scal_data,
            zCol=" runtime_last",
            zColLabel="Time to last solution [s]",
            isLog=True,
            xMax=150,
            drawTimeoutLine=True,
            plot_name="idesyde_time_to_last",
            isMax=False
        )
    # print("-- plotting time-to-last complexity map --")
    # if len(idesyde_data) > 0:
    #     plot_complexity_barriers(
    #         idesyde_scal_data,
    #         zCol=" runtime_last",
    #         zAxisLabel="Time to last",
    #         xMax=150,
    #         plot_name="idesyde_time_to_last_complexity",
    #     )
    # print("-- plotting num sols complexity map --")
    # if len(idesyde_data) > 0:
    #     plot_complexity_barriers(
    #         idesyde_scal_data,
    #         zCol=" nsols",
    #         zAxisLabel="Number of solutions",
    #         xMax=150,
    #         plot_name="idesyde_nsols_complexity",
    #     )
    print("-- plotting average num sols --")
    if len(idesyde_data) > 0:
        plot_firings(
            idesyde_scal_data,
            zCol=" nsols",
            zColLabel="Number of solutions",
            zScale=1,
            xMax=150,
            plot_name="idesyde_nsols",
        )
    # print("-- plotting total run complexity map --")
    # if len(idesyde_data) > 0:
    #     plot_complexity_barriers(
    #         idesyde_scal_data,
    #         zCol=" runtime",
    #         zAxisLabel="Total runtime [ms]",
    #         # zLevels=[
    #         #     1000,
    #         #     1000 * 60,
    #         #     1000 * 1800,
    #         #     1000 * 3600,
    #         #     1000 * 3600 * 8
    #         # ],
    #         # zLabels=[
    #         #     "1 sec",
    #         #     "1 min",
    #         #     "30 mins",
    #         #     "1 hour",
    #         #     "8 hours"
    #         # ],
    #         # is_log=True,
    #         xMax=150,
    #         plot_name="idesyde_total_scal_complexity",
    #     )


if __name__ == "__main__":
    main()
