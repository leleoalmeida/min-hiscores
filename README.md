# Am I There Yet?

A [RuneLite](https://runelite.net) external plugin that shows how much XP each of your skills needs to appear on the [OSRS HiScores](https://secure.runescape.com/m=hiscore_oldschool/overall) (top 2,000,000 players).

## What it does

For each skill, the plugin fetches the XP of the player ranked exactly 2,000,000th and compares it against your current XP. You can see at a glance which skills you're already ranked in, and exactly how much XP you still need for the ones you're not.

- **X xp to go** — you need this much XP before you'll appear on the HiScores for that skill
- **Rank #X** — you're already on the HiScores at this rank
- **< 2M players ranked** — fewer than 2,000,000 players have any XP in this skill, so any XP at all gets you ranked
- **Pending update** — you've hit the cutoff XP but the HiScores haven't refreshed your rank yet

## How to use

1. Log in to OSRS inside the RuneLite client.
2. Open the **Am I There Yet?** panel from the sidebar (checkered flag icon).
3. Click **Load HiScores** — the plugin fetches your stats and the rank-2,000,000 cutoff for every skill.
4. Use the **Sort** button to cycle through sort modes:
   - **Default** — skills in their natural order, Overall pinned at the top
   - **Sort ↓** — skills with the most XP needed first, then worst-ranked skills, then the rest
   - **Sort ↑** — reverse of the above (best rank first, then closest to the cutoff)
5. Tick **Hide ranked** to collapse skills you're already on the HiScores for, so you can focus on what's left.
6. Click **Track** on any unranked skill to set its HiScores cutoff XP as a goal in the **XP Tracker** plugin. The goal will appear in the XP Tracker the next time you gain XP in that skill.
7. Click **Refresh** at any time to re-fetch your stats and the latest cutoff XPs.

## Configuration

| Option | Description |
|--------|-------------|
| HiScores Mode | Which HiScores table to compare against — Normal, Seasonal (DMM), etc. |
