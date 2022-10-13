import pandas as pd
import matplotlib.pyplot as plt
import matplotlib as mpl

### Baseline
golden_ratio = 1.618033988749894
expected_space_per_y_count = 0.3
img_width_in_inches = 3.5

## global style configuration
mpl.rcParams.update({
    "text.usetex": True,
    "font.family": "Computer Modern Roman",
    'font.size': 11
})

idesyde_data = pd.read_csv('idesyde_benchmark.csv')
desyde_data = pd.read_csv("desyde_benchmark.csv")

desyde_agg = desyde_data.groupby(['plat', ' actors']) # the space before actors is significant because of header names
idesyde_agg = idesyde_data.groupby(['plat', ' actors']) # the space before actors is significant because of header names

print(desyde_agg.quantile([0.2, 0.5, 0.8]))
print(idesyde_agg.quantile([0.2, 0.5, 0.8]))