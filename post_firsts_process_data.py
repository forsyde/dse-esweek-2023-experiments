import csv

def plot_firsts():
    idesyde_data = pd.read_csv('idesyde_benchmark.csv')
    desyde_data = pd.read_csv("desyde_benchmark.csv")

    desyde_firsts = desyde_data.groupby(['plat', ' actors', ' exp'])[' start'].max()
    idesyde_firsts = idesyde_data.groupby(['plat', ' actors', ' exp'])[' start'].max()

    with open('idesyde_firsts_benchmark.csv') as idesyde_f:
        writer = csv.writer(idesyde_f, delimiter=',', quotechar='"', quoting=csv.QUOTE_MINIMAL)
        writer.writerow(['plat', 'actors', 'exp', 'start', 'first', 'duration'])
        for (plat, actors, exp) in idesyde_firsts.index:
            output_folder = pathlib.Path('sdfComparison') / "plat_{0}_actors_{1}".format(plat, actors) / "hsdf_{0}".format(exp) / "idesyde_output"
            start_time = datetime.fromisoformat(idesyde_firsts.loc[plat, actors, exp].strip()[:26])
            first_solution = min(output_folder.glob("solution*"), key=lambda f: f.stat().st_mtime)
            first_found = datetime.fromtimestamp(first_solution.stat().st_mtime)
            duration = first_found - start_time
            writer.writerow([plat, actors, exp, start_time, first_found, duration])


def main():
    plot_firsts()

if __name__ == "__main__":
    main()