import { test, expect } from '@playwright/test'

const API = 'http://localhost:8080'

test.describe('Báo cáo phổ điểm (ReportPage)', () => {
  test('renders the band-count chart with a bar per subject and a legend per band', async ({ page }) => {
    await page.route(`${API}/api/report/band-counts`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          { maMon: 'toan', tenMon: 'Toán', gioi: 10, kha: 20, trungBinh: 30, yeu: 5 },
          { maMon: 'ngu_van', tenMon: 'Ngữ văn', gioi: 8, kha: 15, trungBinh: 25, yeu: 2 },
        ]),
      })
    })

    await page.goto('/report')

    await expect(page.getByRole('application')).toBeVisible()
    await expect(page.getByText('Giỏi (≥ 8)')).toBeVisible()
    await expect(page.getByText('Khá (6 - 8)')).toBeVisible()
    await expect(page.getByText('Trung bình (4 - 6)')).toBeVisible()
    await expect(page.getByText('Yếu (< 4)')).toBeVisible()
    await expect(page.getByText('Toán', { exact: true })).toBeVisible()
  })

  test('shows the error message when the report request fails', async ({ page }) => {
    await page.route(`${API}/api/report/band-counts`, async (route) => {
      await route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({ message: 'Lỗi máy chủ' }),
      })
    })

    await page.goto('/report')

    await expect(page.getByText('Lỗi máy chủ')).toBeVisible()
  })
})
