
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
    data = []

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
            raise ValueError(f"Unexpected CSV columns in {filename}")

        for row in reader:
            data.append({
                "nrMessages": int(row["nrMessages"]),
                "latency": float(row["latency"]),
                "throughput": float(row["throughput"]),
                "bandwidth": float(row["bandwidth"]),
                "nrNodes": int(row["nrNodes"]),
                "payloadSize": int(row["payloadSize"]),
            })

    return data


def read_repetitions(prefix):
    """Read available repetitions, e.g. amount_scaling.log0."""
    repetitions = []

    for i in range(REPETITIONS):
        filename = os.path.join("logfiles", f"{prefix}{i}")

        if not os.path.exists(filename):
            print(f"Warning: {filename} not found")
            continue

        run = read_log(filename)

        if run:
            repetitions.append(run)

    if not repetitions:
        raise FileNotFoundError(
            f"No log files found for prefix '{prefix}'"
        )

    # All repetitions should contain the same configurations.
    reference = [
        (row["nrMessages"], row["payloadSize"], row["nrNodes"])
        for row in repetitions[0]
    ]

    for run in repetitions[1:]:
        configurations = [
            (row["nrMessages"], row["payloadSize"], row["nrNodes"])
            for row in run
        ]

        if configurations != reference:
            raise ValueError(
                f"Inconsistent configurations for prefix '{prefix}'"
            )

    print(f"{prefix}: loaded {len(repetitions)} repetitions")
    return repetitions


# The Java test writes amount_scaling.log0, size_scaling.log0, etc.
amount_runs = read_repetitions("amount_scaling.log")
size_runs = read_repetitions("size_scaling.log")
node_runs = read_repetitions("node_scaling.log")


# ------------------------------------------------------------
# Calculate averages
# ------------------------------------------------------------

def average_runs(repetitions, metric):
    """Average each configuration across all available repetitions."""
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
):
    x = get_x_values(repetitions, x_field)
    averages = average_runs(repetitions, metric)

    plt.figure()
    plt.plot(x, averages, marker="o", label="Average")

    plt.xlabel(x_label)
    plt.ylabel(y_label)
    plt.title(title)
    plt.grid(True)
    plt.legend()
    plt.tight_layout()
    plt.savefig(os.path.join("plots", filename), dpi=300)
    plt.close()


METRICS = [
    ("latency", "Latency (ms)", "latency"),
    ("throughput", "Throughput (quizzes/s)", "throughput"),
    ("bandwidth", "Bandwidth (bytes/s)", "bandwidth"),
]


# Amount scaling: vary the number of quizzes.
for metric, ylabel, suffix in METRICS:
    plot_scaling(
        amount_runs,
        "nrMessages",
        "Number of quizzes",
        metric,
        ylabel,
        f"{ylabel} - Amount Scaling",
        f"amount_{suffix}.png",
    )


# Size scaling: vary the payload size per quiz.
for metric, ylabel, suffix in METRICS:
    plot_scaling(
        size_runs,
        "payloadSize",
        "Payload size per quiz (bytes)",
        metric,
        ylabel,
        f"{ylabel} - Payload Size Scaling",
        f"size_{suffix}.png",
    )


# Node scaling: vary the number of nodes.
for metric, ylabel, suffix in METRICS:
    plot_scaling(
        node_runs,
        "nrNodes",
        "Number of nodes",
        metric,
        ylabel,
        f"{ylabel} - Node Scaling",
        f"nodes_{suffix}.png",
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
):
    """Each box shows the repetitions for one configuration."""
    x_values = get_x_values(repetitions, x_field)

    data_sets = [
        [run[i][metric] for run in repetitions]
        for i in range(len(x_values))
    ]

    plt.figure()
    plt.boxplot(data_sets, tick_labels=[str(x) for x in x_values])

    plt.xlabel(x_field)
    plt.ylabel(y_label)
    plt.title(title)
    plt.grid(axis="y")
    plt.tight_layout()
    plt.savefig(os.path.join("plots", filename), dpi=300)
    plt.close()


BOX_PLOT_CONFIGS = [
    (
        amount_runs,
        "nrMessages",
        "Amount Scaling",
        "amount",
    ),
    (
        size_runs,
        "payloadSize",
        "Payload Size Scaling",
        "size",
    ),
    (
        node_runs,
        "nrNodes",
        "Node Scaling",
        "nodes",
    ),
]

for runs, x_field, experiment, prefix in BOX_PLOT_CONFIGS:
    for metric, ylabel, suffix in METRICS:
        make_boxplots(
            runs,
            x_field,
            metric,
            ylabel,
            f"{ylabel} - {experiment}",
            f"boxplot_{prefix}_{suffix}.png",
        )


print("Plots saved to the plots/ directory.")
