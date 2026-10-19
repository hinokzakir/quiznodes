
import csv
import os
import statistics
import matplotlib.pyplot as plt

os.makedirs("plots", exist_ok=True)

REPETITIONS = 7


# ------------------------------------------------------------
# Reading log files
# ------------------------------------------------------------

def read_log(filename):
    """Read a log file, returning an empty list if missing or empty."""
    if not os.path.isfile(filename) or os.path.getsize(filename) == 0:
        print(f"Skipping missing or empty file: {filename}")
        return []

    data = []

    try:
        with open(filename, newline="") as file:
            reader = csv.DictReader(file)

            required_columns = {
                "nrMessages",
                "latency",
                "throughput",
                "bandwidth",
                "nrNodes",
                "payloadSize",
            }

            if not required_columns.issubset(reader.fieldnames or []):
                print(f"Skipping file with missing or invalid columns: {filename}")
                return []

            for row in reader:
                if not row or any(
                    row.get(column) in (None, "")
                    for column in required_columns
                ):
                    continue

                try:
                    data.append({
                        "nrMessages": int(row["nrMessages"]),
                        "latency": float(row["latency"]),
                        "throughput": float(row["throughput"]),
                        "bandwidth": float(row["bandwidth"]),
                        "nrNodes": int(row["nrNodes"]),
                        "payloadSize": int(row["payloadSize"]),
                    })
                except (ValueError, TypeError):
                    print(f"Skipping invalid row in {filename}: {row}")

    except (OSError, csv.Error) as e:
        print(f"Skipping unreadable file {filename}: {e}")
        return []

    if not data:
        print(f"Skipping file with no valid data rows: {filename}")

    return data


def read_repetitions(prefix):
    """Read available non-empty repetitions, e.g. amount_scaling.log0."""
    repetitions = []

    for i in range(REPETITIONS):
        filename = os.path.join("logfiles", f"{prefix}{i}")
        run = read_log(filename)

        if run:
            repetitions.append(run)

    if not repetitions:
        print(f"No usable log files found for '{prefix}'. Skipping experiment.")
        return []

    # All repetitions should contain the same configurations.
    reference = [
        (row["nrMessages"], row["payloadSize"], row["nrNodes"])
        for row in repetitions[0]
    ]

    consistent_repetitions = [repetitions[0]]

    for run in repetitions[1:]:
        configurations = [
            (row["nrMessages"], row["payloadSize"], row["nrNodes"])
            for row in run
        ]

        if configurations != reference:
            print(
                f"Skipping inconsistent repetition for '{prefix}'. "
                "Its configurations differ from the first usable repetition."
            )
            continue

        consistent_repetitions.append(run)

    print(
        f"{prefix}: loaded {len(consistent_repetitions)} "
        "usable repetitions"
    )
    return consistent_repetitions


# Load whatever data is available.
amount_runs = read_repetitions("amount_scaling.log")
size_runs = read_repetitions("size_scaling.log")
node_runs = read_repetitions("node_scaling.log")


# ------------------------------------------------------------
# Calculate averages
# ------------------------------------------------------------

def average_runs(repetitions, metric):
    """Average each configuration across available repetitions."""
    return [
        statistics.mean(run[i][metric] for run in repetitions)
        for i in range(len(repetitions[0]))
    ]


def get_x_values(repetitions, field):
    """Read x-axis values from the actual log data."""
    return [row[field] for row in repetitions[0]]


# ------------------------------------------------------------
# Line plots
# ------------------------------------------------------------

def plot_scaling(
    repetitions,
    x_field,
    x_label,
    metric,
    y_label,
    title,
    filename,
    log_x=False,
):
    if not repetitions:
        return

    x = get_x_values(repetitions, x_field)
    averages = average_runs(repetitions, metric)

    plt.figure()
    plt.plot(x, averages, marker="o", label="Average")

    if log_x:
        plt.xscale("log", base=10)

    # Start the y-axis at zero to avoid exaggerating differences.
    plt.ylim(bottom=0)

    plt.xlabel(x_label)
    plt.ylabel(y_label)
    plt.title(title)
    plt.grid(True, which="both")
    plt.legend()
    plt.tight_layout()
    plt.savefig(os.path.join("plots", filename), dpi=300)
    plt.close()


METRICS = [
    ("latency", "Latency (ms)", "latency"),
    ("throughput", "Throughput (quizzes/s)", "throughput"),
    ("bandwidth", "Bandwidth (bytes/s)", "bandwidth"),
]


LINE_PLOT_CONFIGS = [
    (
        amount_runs,
        "nrMessages",
        "Number of quizzes",
        "Amount Scaling",
        "amount",
    ),
    (
        size_runs,
        "payloadSize",
        "Payload size per quiz (bytes)",
        "Payload Size Scaling",
        "size",
    ),
    (
        node_runs,
        "nrNodes",
        "Number of nodes",
        "Node Scaling",
        "nodes",
    ),
]

for runs, x_field, x_label, experiment, prefix in LINE_PLOT_CONFIGS:
    if not runs:
        print(f"Skipping line plots for {experiment}: no usable data.")
        continue

    for metric, ylabel, suffix in METRICS:
        plot_scaling(
            repetitions=runs,
            x_field=x_field,
            x_label=x_label,
            metric=metric,
            y_label=ylabel,
            title=f"{ylabel} - {experiment}",
            filename=f"{prefix}_{suffix}.png",
            log_x=(prefix == "size"),
        )


# ------------------------------------------------------------
# Box plots across repetitions
# ------------------------------------------------------------

def make_boxplots(
    repetitions,
    x_field,
    metric,
    y_label,
    title,
    filename,
    log_x=False,
):
    """Each box shows the available repetitions for one configuration."""
    if not repetitions:
        return

    x_values = get_x_values(repetitions, x_field)

    data_sets = [
        [run[i][metric] for run in repetitions]
        for i in range(len(x_values))
    ]

    plt.figure()

    if log_x:
        # Position each box at its actual payload size.
        positions = x_values
        widths = [max(value * 0.12, 1) for value in positions]

        plt.boxplot(
            data_sets,
            positions=positions,
            widths=widths,
        )
        plt.xscale("log", base=10)
        plt.xticks(positions, [str(x) for x in x_values])
    else:
        plt.boxplot(
            data_sets,
            tick_labels=[str(x) for x in x_values],
        )

    # Start the y-axis at zero to avoid exaggerating differences.
    plt.ylim(bottom=0)

    plt.xlabel(x_field)
    plt.ylabel(y_label)
    plt.title(title)
    plt.grid(axis="y", which="both")
    plt.tight_layout()
    plt.savefig(os.path.join("plots", filename), dpi=300)
    plt.close()


BOX_PLOT_CONFIGS = [
    (amount_runs, "nrMessages", "Amount Scaling", "amount"),
    (size_runs, "payloadSize", "Payload Size Scaling", "size"),
    (node_runs, "nrNodes", "Node Scaling", "nodes"),
]

for runs, x_field, experiment, prefix in BOX_PLOT_CONFIGS:
    if not runs:
        print(f"Skipping box plots for {experiment}: no usable data.")
        continue

    for metric, ylabel, suffix in METRICS:
        make_boxplots(
            repetitions=runs,
            x_field=x_field,
            metric=metric,
            y_label=ylabel,
            title=f"{ylabel} - {experiment}",
            filename=f"boxplot_{prefix}_{suffix}.png",
            log_x=(prefix == "size"),
        )


print("Plot generation finished. Check the plots/ directory.")

