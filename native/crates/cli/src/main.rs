//! Jinshu tenant CLI - skeleton only.

fn main() -> anyhow::Result<()> {
    tracing_subscriber::fmt::init();
    tracing::info!("jinshu-cli {} (skeleton)", env!("CARGO_PKG_VERSION"));
    Ok(())
}
