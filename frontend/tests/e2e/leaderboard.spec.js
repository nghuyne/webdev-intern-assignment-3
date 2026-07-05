import { test, expect } from '@playwright/test'

const API = 'http://localhost:8080'

test.describe('Bảng xếp hạng khối A (LeaderboardPage)', () => {
  test('renders ranked rows with medal styling for the top 3', async ({ page }) => {
    await page.route(`${API}/api/leaderboard/group-a`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          { hang: 1, sbd: '10000001', tongDiem: 29.5 },
          { hang: 2, sbd: '10000002', tongDiem: 28.0 },
          { hang: 3, sbd: '10000003', tongDiem: 27.5 },
          { hang: 4, sbd: '10000004', tongDiem: 27.0 },
        ]),
      })
    })

    await page.goto('/leaderboard')

    const rows = page.locator('tbody tr')
    await expect(rows).toHaveCount(4)
    await expect(rows.nth(0)).toContainText('10000001')
    await expect(rows.nth(0)).toContainText('29.5')
    await expect(rows.nth(3)).toContainText('10000004')

    // Rank 1-3 get a medal-colored badge; rank 4 falls back to the plain slate badge.
    const rank1Badge = rows.nth(0).locator('span').first()
    await expect(rank1Badge).toHaveCSS('background-color', 'rgb(251, 232, 166)')
  })

  test('shows an empty table body when no candidates qualify', async ({ page }) => {
    await page.route(`${API}/api/leaderboard/group-a`, async (route) => {
      await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' })
    })

    await page.goto('/leaderboard')

    await expect(page.locator('table')).toBeVisible()
    await expect(page.locator('tbody tr')).toHaveCount(0)
  })
})
