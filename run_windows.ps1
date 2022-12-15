$Process = Start-Process "amm" -ArgumentList "run_experiments.sc", "evaluation_2_idesyde" -PassThru -RedirectStandardError "errors.txt" -RedirectStandardOutput "outputs.txt" -NoNewWindow
$Process.PriorityClass = "High"
New-Item ("running_" + $Process.Id + ".lock")
