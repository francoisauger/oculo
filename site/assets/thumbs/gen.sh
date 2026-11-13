#!/bin/zsh
# Generates the invented video thumbnails used by the site's TV demo (Codex CLI image generation).
# No text, no logos, no recognizable people: nothing here comes from YouTube.
gen() {
  [[ -f "$1.png" ]] && return
  codex exec --skip-git-repo-check -s workspace-write "Generate one image: a 16:9 photographic still that looks like a frame from a high-quality video, $2. No text, no captions, no logos, no watermarks, no recognizable people. Save it in the current directory as $1.png" < /dev/null > "$1.log" 2>&1
}
gen aurora "green and teal northern lights over a small snowy cabin at night, wide shot"
gen ramen "close-up of a steaming bowl of ramen on a dark wooden counter, warm orange light"
gen reef "an underwater coral reef at dawn with sun rays through deep blue water and a school of small fish"
gen canyon "a tiny mountain biker riding a ridge in a vast red sandstone canyon at golden hour"
gen vinyl "a vinyl record spinning on a turntable in a dim room lit by purple and magenta neon light, shallow depth of field"
gen workshop "a woodworking bench with a hand plane and curls of golden oak shavings, warm yellow workshop light"
gen city "a long-exposure night city avenue with pink and violet light trails and wet reflections"
gen rocket "a rocket standing on a launch pad at dusk with floodlights and vapor, deep blue sky with an orange horizon"
