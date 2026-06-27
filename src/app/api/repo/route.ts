import { NextResponse } from 'next/server';
import { exec } from 'child_process';
import { promisify } from 'util';
import { existsSync } from 'fs';
import { join } from 'path';

const execAsync = promisify(exec);

const REPOS_DIR = '/tmp/kilo-repos';

function ensureReposDir() {
  const { mkdirSync } = require('fs');
  if (!existsSync(REPOS_DIR)) {
    mkdirSync(REPOS_DIR, { recursive: true });
  }
}

function sanitizeRepoName(name: string): string {
  // Convert "owner/repo" to safe directory name
  return name.replace(/[^a-zA-Z0-9._-]/g, '_');
}

export async function POST(request: Request) {
  try {
    const { action, repo } = await request.json();

    if (!action || !repo) {
      return NextResponse.json(
        { error: 'action and repo are required' },
        { status: 400 }
      );
    }

    if (action !== 'clone' && action !== 'reopen') {
      return NextResponse.json(
        { error: 'action must be "clone" or "reopen"' },
        { status: 400 }
      );
    }

    // Validate repo format (owner/repo)
    if (!/^[a-zA-Z0-9._-]+\/[a-zA-Z0-9._-]+$/.test(repo)) {
      return NextResponse.json(
        { error: 'repo must be in "owner/repo" format' },
        { status: 400 }
      );
    }

    ensureReposDir();
    const repoDir = join(REPOS_DIR, sanitizeRepoName(repo));

    if (action === 'reopen') {
      // Check if repo already exists
      if (existsSync(repoDir)) {
        return NextResponse.json({
          success: true,
          path: repoDir,
          message: 'Repository already cloned',
          alreadyCloned: true,
        });
      }

      return NextResponse.json(
        { error: 'Repository not found. Clone it first.' },
        { status: 404 }
      );
    }

    // action === 'clone'
    if (existsSync(repoDir)) {
      return NextResponse.json({
        success: true,
        path: repoDir,
        message: 'Repository already cloned',
        alreadyCloned: true,
      });
    }

    // Clone using gh CLI (must be authenticated via /api/auth/github first)
    const { stdout, stderr } = await execAsync(
      `gh repo clone "${repo}" "${repoDir}" -- --depth 1`,
      { timeout: 120000 }
    );

    if (!existsSync(join(repoDir, '.git'))) {
      return NextResponse.json(
        { error: 'Clone failed', details: stderr || stdout },
        { status: 500 }
      );
    }

    return NextResponse.json({
      success: true,
      path: repoDir,
      message: 'Repository cloned successfully',
      alreadyCloned: false,
    });
  } catch (error: any) {
    console.error('Repo operation error:', error);

    // Check for specific gh errors
    const errMsg = error.message || '';
    if (errMsg.includes('not authenticated') || errMsg.includes('gh auth')) {
      return NextResponse.json(
        { error: 'GitHub not authenticated. Please authenticate first via /api/auth/github' },
        { status: 401 }
      );
    }
    if (errMsg.includes('Could not resolve') || errMsg.includes('404')) {
      return NextResponse.json(
        { error: 'Repository not found or not accessible' },
        { status: 404 }
      );
    }

    return NextResponse.json(
      { error: 'Failed to process repository', details: errMsg },
      { status: 500 }
    );
  }
}

export async function GET(request: Request) {
  try {
    const url = new URL(request.url);
    const searchQuery = url.searchParams.get('q')?.trim();

    if (!searchQuery) {
      ensureReposDir();
      const { readdirSync, statSync } = require('fs');

      const entries = readdirSync(REPOS_DIR)
        .filter((name: string) => !name.startsWith('.'))
        .map((name: string) => {
          const fullPath = join(REPOS_DIR, name);
          try {
            const stat = statSync(fullPath);
            return {
              name,
              path: fullPath,
              modified: stat.mtime.toISOString(),
              source: 'local',
            };
          } catch {
            return null;
          }
        })
        .filter(Boolean);

      return NextResponse.json({ repos: entries, source: 'local' });
    }

    // Search GitHub using gh CLI
    const { stdout } = await execAsync(
      `gh search repos "${searchQuery}" --limit 30 --json name,owner,description,stargazersCount,updatedAt`,
      { timeout: 30000 }
    );

    if (!stdout || stdout.trim() === '') {
      return NextResponse.json({ repos: [], source: 'github' });
    }

    const results = JSON.parse(stdout);
    const repos = results.map((item: any) => ({
      name: item.owner?.login ? `${item.owner.login}/${item.name}` : item.name,
      description: item.description || '',
      stars: item.stargazersCount || 0,
      updated: item.updatedAt || '',
      source: 'github',
    }));

    return NextResponse.json({ repos, source: 'github' });
  } catch (error: any) {
    console.error('Search repos error:', error);

    const errMsg = error.message || '';
    if (errMsg.includes('not authenticated') || errMsg.includes('gh auth')) {
      return NextResponse.json(
        { error: 'GitHub not authenticated. Please authenticate first via /api/auth/github' },
        { status: 401 }
      );
    }

    return NextResponse.json(
      { error: 'Failed to search repositories', details: errMsg },
      { status: 500 }
    );
  }
}
