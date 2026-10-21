# Neural players are trained in Python and run in the JVM

A learned player is a model trained offline in Python, exported to ONNX, and executed
in the JVM by a single-threaded ONNX Runtime session. The rules module knows nothing
about it.

## Considered options

- **Python drives the game over a socket.** The trainer would act in real time, but the
  network and the round-trip latency become part of the training loop, and the rules
  would need a server mode.
- **The rules module produces ready-made vectors.** Faster to consume, but the rules
  would acquire a machine-learning contract and a fixed feature layout that has to
  change whenever the model changes.
- **The model only trains and never plays.** It could learn from other players' games,
  but it could never be measured against them.

## Consequences

- The observation lives in a separate module (`ml`), as the only place where "what the
  model sees" is decided. Changing the model changes that module, not the rules.
- The ONNX session runs single-threaded. Multi-threaded inference in ONNX Runtime is
  not bit-reproducible — different reduction orders can produce slightly different
  logits, and a borderline maximum can then choose a different card. For a model of a
  few hundred floats, one thread costs almost nothing and buys exact reproducibility.
- An episode is reproducible from its setup, its policy and its recorded moves. Which
  policy produced it is recorded alongside it; the golden scenarios use the fixed rule
  bot and never a learned model.
- The rules stay free of a machine-learning dependency, so the Android release build
  does not have to shrink a model runtime it does not use.
