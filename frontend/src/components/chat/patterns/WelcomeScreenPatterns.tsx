// 未使用だが、将来の可能性を残す

// Pattern 2: Typographic Excellence
export function PatternTypo({ buildTime }: { buildTime?: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-6 w-full animate-slide-up">
      <div className="relative">
        <div className="absolute -top-10 -left-10 w-20 h-[1px] bg-gradient-to-r from-transparent to-border" />
        <div className="absolute -bottom-10 -right-10 w-20 h-[1px] bg-gradient-to-l from-transparent to-border" />

        <h1 className="text-6xl md:text-8xl font-black tracking-tighter text-transparent bg-clip-text bg-gradient-to-br from-foreground via-foreground to-accent/50 selection:bg-accent selection:text-accent-foreground">
          EGO
          <br />
          GRAPH
        </h1>
      </div>

      <div className="flex items-center gap-4 text-sm font-mono text-muted-foreground border-t border-b border-border/50 py-3 px-8">
        <span>DATA</span>
        <span className="text-border">•</span>
        <span>WAREHOUSE</span>
        <span className="text-border">•</span>
        <span>AI</span>
      </div>

      {buildTime && (
        <div className="absolute bottom-8 left-8 font-mono text-[10px] text-muted-foreground/30">
          SYS.VER: {buildTime}
        </div>
      )}
    </div>
  );
}

// Pattern 3: Network/Nodes
export function PatternNetwork({ buildTime }: { buildTime?: string }) {
  return (
    <div className="flex flex-col items-center gap-10 w-full max-w-md animate-fade-in">
      <div className="relative w-40 h-40">
        <div className="absolute inset-0 border border-accent/20 rounded-full animate-[spin_8s_linear_infinite]" />
        <div className="absolute inset-4 border border-accent/10 rounded-full animate-[spin_12s_linear_infinite_reverse]" />

        <div className="absolute inset-0 flex items-center justify-center">
          <div className="w-4 h-4 bg-accent rounded-full shadow-[0_0_20px_currentColor] z-10" />
          <div className="absolute w-24 h-24 animate-[spin_4s_linear_infinite]">
            <div className="absolute top-0 left-1/2 -translate-x-1/2 w-2 h-2 bg-foreground rounded-full" />
          </div>
          <div className="absolute w-16 h-16 animate-[spin_3s_linear_infinite_reverse]">
            <div className="absolute bottom-0 left-1/2 -translate-x-1/2 w-1.5 h-1.5 bg-muted-foreground rounded-full" />
          </div>
        </div>
      </div>

      <div className="text-center space-y-2">
        <h2 className="text-xl font-medium tracking-wide text-foreground uppercase">
          System Standby
        </h2>
        <p className="text-sm text-muted-foreground max-w-xs mx-auto">
          Initialize conversation to retrieve or ingest data points.
        </p>
      </div>

      {buildTime && (
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-secondary/50 border border-border/50">
          <div className="w-1.5 h-1.5 rounded-full bg-green-500 animate-pulse" />
          <span className="font-mono text-[10px] text-muted-foreground">
            ONLINE {buildTime}
          </span>
        </div>
      )}
    </div>
  );
}
