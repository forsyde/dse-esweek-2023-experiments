import csv
from datetime import datetime
import pathlib

def plot_firsts():
    # idesyde_data = pd.read_csv('idesyde_benchmark.csv')
    # desyde_data = pd.read_csv("desyde_benchmark.csv")
    root = pathlib.Path("sdfComparison")
    with open('desyde_benchmark.csv') as inputcsv:
        reader = csv.reader(inputcsv, delimiter=',', quotechar='"')
        next(reader, None)
        with open('desyde_benchmark_firsts.csv', 'w') as idesyde_f:
            writer = csv.writer(idesyde_f, delimiter=',', quotechar='"', quoting=csv.QUOTE_MINIMAL)
            writer.writerow(['plat', 'actors', 'exp', 'start', 'first', 'duration'])
            for row in reader:
                plat = row[0]
                actors = row[1].strip()
                exp = row[2].strip()
                start_time = datetime.fromisoformat(row[3].strip()[:26])
                output_folder = root / "plat_{0}_actors_{1}".format(plat, actors) / "hsdf_{0}".format(exp) / "idesyde_output"
                if len(list(output_folder.glob("solution*"))) > 0:
                    first_solution = min(output_folder.glob("solution*"), default=datetime.now(), key=lambda f: f.stat().st_mtime)
                    first_found = datetime.fromtimestamp(first_solution.stat().st_mtime)
                    duration = first_found - start_time
                    writer.writerow([plat, actors, exp, start_time, first_found, duration.total_seconds()])

def main():
    plot_firsts()

if __name__ == "__main__":
    main()