# Public benchmark validation

Reference observations are evaluated with each subject excluded. These are activity ranks, not quality labels.

## 2026-08-01T00:00:00Z to 2026-09-01T00:00:00Z (end excluded)

Dataset: `0xb1ddf031b73d0f53ade08c58b283c2b61183ff495a5b8603537dcdf31a694f95`

| Repository | Language | Active contributors |
|---|---|---:|
| CommunityToolkit/dotnet | C# | 2 |
| Cysharp/MemoryPack | C# | 0 |
| Cysharp/UniTask | C# | 0 |
| DapperLib/Dapper | C# | 4 |
| Tyrrrz/CliWrap | C# | 1 |
| spectreconsole/spectre.console | C# | 1 |

Result counts: {INSUFFICIENT_COHORT=8}

Monotonicity violations after adding one commit: 0.

## 2026-06-01T00:00:00Z to 2026-09-01T00:00:00Z (end excluded)

Dataset: `0x3cb7ab96f6e3e8abda4c6790b83a324b94bf1074210f391e5efedf3b0244b754`

| Repository | Language | Active contributors |
|---|---|---:|
| CommunityToolkit/Maui | C# | 13 |
| CommunityToolkit/dotnet | C# | 5 |
| Cysharp/MemoryPack | C# | 7 |
| Cysharp/UniTask | C# | 4 |
| DapperLib/Dapper | C# | 8 |
| FluentValidation/FluentValidation | C# | 9 |
| Tyrrrz/CliWrap | C# | 3 |
| dotnet/command-line-api | C# | 14 |
| reactiveui/ReactiveUI | C# | 5 |
| restsharp/RestSharp | C# | 6 |
| serilog/serilog | C# | 2 |
| spectreconsole/spectre.console | C# | 14 |

Result counts: {AVAILABLE=90}

Observed percentile range: 0.0 to 100.0; middle observation: 36.9.

| Percentile band | Observations |
|---|---:|
| 0-20 | 7 |
| 20-40 | 42 |
| 40-60 | 7 |
| 60-80 | 17 |
| 80-100 | 17 |

Largest change after adding one commit (other dimensions fixed): 63.6 percentile points.

Monotonicity violations after adding one commit: 0.

Limits: curated convenience sample, not a representative population; repository size and language do not control developer role or workflow. Small samples and ties can produce large rank jumps. The equal-weight model remains experimental and requires broader calibration before use in high-stakes evaluation.
